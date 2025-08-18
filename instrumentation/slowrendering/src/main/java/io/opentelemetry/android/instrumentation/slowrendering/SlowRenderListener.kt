/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.N)
class SlowRenderListener implements DefaultingActivityLifecycleCallbacks {
    private static final HandlerThread frameMetricsThread =
            new HandlerThread("FrameMetricsCollector");

    private final JankReporter jankReporter;
    private final ScheduledExecutorService executorService;
    private final Handler frameMetricsHandler;
    private final Duration pollInterval;

    private final ConcurrentMap<Activity, PerActivityListener> activities =
            new ConcurrentHashMap<>();

    SlowRenderListener(JankReporter jankReporter, Duration pollInterval) {
        this(
                jankReporter,
                Executors.newScheduledThreadPool(1),
                new Handler(startFrameMetricsLoop()),
                pollInterval);
    }

    // Exists for testing
    SlowRenderListener(
            JankReporter jankReporter,
            ScheduledExecutorService executorService,
            Handler frameMetricsHandler,
            Duration pollInterval) {
        this.jankReporter = jankReporter;
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

    public void shutdown() {
        executorService.shutdownNow();
        for (Map.Entry<Activity, PerActivityListener> entry : activities.entrySet()) {
            Activity activity = entry.getKey();
            PerActivityListener listener = entry.getValue();
            activity.getWindow().removeOnFrameMetricsAvailableListener(listener);
        }
        activities.clear();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (executorService.isShutdown()) {
            return;
        }
        PerActivityListener listener = new PerActivityListener(activity);
        PerActivityListener existing = activities.putIfAbsent(activity, listener);
        if (existing == null) {
            activity.getWindow().addOnFrameMetricsAvailableListener(listener, frameMetricsHandler);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (executorService.isShutdown()) {
            return;
        }
        PerActivityListener listener = activities.remove(activity);
        if (listener != null) {
            activity.getWindow().removeOnFrameMetricsAvailableListener(listener);
            jankReporter.reportSlow(listener);
        }
    }

    private void reportSlowRenders() {
        try {
            activities.forEach((activity, listener) -> jankReporter.reportSlow(listener));
        } catch (Exception e) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Exception while processing frame metrics", e);
        }
    }
}
