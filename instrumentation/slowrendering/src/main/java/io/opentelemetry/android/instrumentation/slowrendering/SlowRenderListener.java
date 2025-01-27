/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering;

import static android.view.FrameMetrics.DRAW_DURATION;
import static android.view.FrameMetrics.FIRST_DRAW_FRAME;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.FrameMetrics;
import android.view.Window;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.N)
class SlowRenderListener implements DefaultingActivityLifecycleCallbacks {

    static final int SLOW_THRESHOLD_MS = 16;
    static final int FROZEN_THRESHOLD_MS = 700;

    private static final int NANOS_PER_MS = (int) TimeUnit.MILLISECONDS.toNanos(1);
    // rounding value adds half a millisecond, for rounding to nearest ms
    private static final int NANOS_ROUNDING_VALUE = NANOS_PER_MS / 2;

    private static final HandlerThread frameMetricsThread =
            new HandlerThread("FrameMetricsCollector");

    private final Tracer tracer;
    private final ScheduledExecutorService executorService;
    private final Handler frameMetricsHandler;
    private final Duration pollInterval;

    private final ConcurrentMap<Activity, PerActivityListener> activities =
            new ConcurrentHashMap<>();

    SlowRenderListener(Tracer tracer, Duration pollInterval) {
        this(
                tracer,
                Executors.newScheduledThreadPool(1),
                new Handler(startFrameMetricsLoop()),
                pollInterval);
    }

    // Exists for testing
    SlowRenderListener(
            Tracer tracer,
            ScheduledExecutorService executorService,
            Handler frameMetricsHandler,
            Duration pollInterval) {
        this.tracer = tracer;
        this.executorService = executorService;
        this.frameMetricsHandler = frameMetricsHandler;
        this.pollInterval = pollInterval;
    }

    private static Looper startFrameMetricsLoop() {
        // just a precaution: this is supposed to be called only once, and the thread should always
        // be not started here
        if (!frameMetricsThread.isAlive()) {
            frameMetricsThread.start();
        }
        return frameMetricsThread.getLooper();
    }

    // the returned future is very unlikely to fail
    @SuppressWarnings("FutureReturnValueIgnored")
    void start() {
        executorService.scheduleWithFixedDelay(
                this::reportSlowRenders,
                pollInterval.toMillis(),
                pollInterval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        PerActivityListener listener = new PerActivityListener(activity);
        PerActivityListener existing = activities.putIfAbsent(activity, listener);
        if (existing == null) {
            activity.getWindow().addOnFrameMetricsAvailableListener(listener, frameMetricsHandler);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        PerActivityListener listener = activities.remove(activity);
        if (listener != null) {
            activity.getWindow().removeOnFrameMetricsAvailableListener(listener);
            reportSlow(listener);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static class PerActivityListener implements Window.OnFrameMetricsAvailableListener {

        private final Activity activity;
        private final Object lock = new Object();

        @GuardedBy("lock")
        private SparseIntArray drawDurationHistogram = new SparseIntArray();

        PerActivityListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onFrameMetricsAvailable(
                Window window, FrameMetrics frameMetrics, int dropCountSinceLastInvocation) {

            long firstDrawFrame = frameMetrics.getMetric(FIRST_DRAW_FRAME);
            if (firstDrawFrame == 1) {
                return;
            }

            long drawDurationsNs = frameMetrics.getMetric(DRAW_DURATION);
            // ignore values < 0; something must have gone wrong
            if (drawDurationsNs >= 0) {
                synchronized (lock) {
                    // calculation copied from FrameMetricsAggregator
                    int durationMs =
                            (int) ((drawDurationsNs + NANOS_ROUNDING_VALUE) / NANOS_PER_MS);
                    int oldValue = drawDurationHistogram.get(durationMs);
                    drawDurationHistogram.put(durationMs, (oldValue + 1));
                }
            }
        }

        SparseIntArray resetMetrics() {
            synchronized (lock) {
                SparseIntArray metrics = drawDurationHistogram;
                drawDurationHistogram = new SparseIntArray();
                return metrics;
            }
        }

        public String getActivityName() {
            return activity.getComponentName().flattenToShortString();
        }
    }

    private void reportSlowRenders() {
        try {
            activities.forEach((activity, listener) -> reportSlow(listener));
        } catch (Exception e) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Exception while processing frame metrics", e);
        }
    }

    private void reportSlow(PerActivityListener listener) {
        int slowCount = 0;
        int frozenCount = 0;
        SparseIntArray durationToCountHistogram = listener.resetMetrics();
        for (int i = 0; i < durationToCountHistogram.size(); i++) {
            int duration = durationToCountHistogram.keyAt(i);
            int count = durationToCountHistogram.get(duration);
            if (duration > FROZEN_THRESHOLD_MS) {
                Log.d(
                        RumConstants.OTEL_RUM_LOG_TAG,
                        "* FROZEN RENDER DETECTED: " + duration + " ms." + count + " times");
                frozenCount += count;
            } else if (duration > SLOW_THRESHOLD_MS) {
                Log.d(
                        RumConstants.OTEL_RUM_LOG_TAG,
                        "* Slow render detected: " + duration + " ms. " + count + " times");
                slowCount += count;
            }
        }

        Instant now = Instant.now();
        if (slowCount > 0) {
            makeSpan("slowRenders", listener.getActivityName(), slowCount, now);
        }
        if (frozenCount > 0) {
            makeSpan("frozenRenders", listener.getActivityName(), frozenCount, now);
        }
    }

    private void makeSpan(String spanName, String activityName, int slowCount, Instant now) {
        Span span =
                tracer.spanBuilder(spanName)
                        .setAttribute("count", slowCount)
                        .setAttribute("activity.name", activityName)
                        .setStartTimestamp(now)
                        .startSpan();
        span.end(now);
    }
}
