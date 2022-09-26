/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import static com.splunk.rum.SplunkRum.LOG_TAG;

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
import androidx.annotation.RequiresApi;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.N)
class SlowRenderingDetectorImpl
        implements SlowRenderingDetector, Window.OnFrameMetricsAvailableListener {

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

    private final Object lock = new Object();

    @GuardedBy("lock")
    private final Set<Activity> activities = new HashSet<>();

    @GuardedBy("lock")
    private SparseIntArray drawDurationHistogram = new SparseIntArray();

    SlowRenderingDetectorImpl(Tracer tracer, Duration pollInterval) {
        this(
                tracer,
                Executors.newScheduledThreadPool(1),
                new Handler(startFrameMetricsLoop()),
                pollInterval);
    }

    // Exists for testing
    SlowRenderingDetectorImpl(
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

    @Override
    public void add(Activity activity) {
        boolean added;
        synchronized (lock) {
            added = activities.add(activity);
        }
        if (added) {
            activity.getWindow().addOnFrameMetricsAvailableListener(this, frameMetricsHandler);
        }
    }

    @Override
    public void stop(Activity activity) {
        boolean removed;
        synchronized (lock) {
            removed = activities.remove(activity);
        }
        if (removed) {
            activity.getWindow().removeOnFrameMetricsAvailableListener(this);
        }
        reportSlow(getMetrics());
    }

    // the returned future is very unlikely to fail
    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    public void start() {
        executorService.scheduleAtFixedRate(
                this::reportSlowRenders,
                pollInterval.toMillis(),
                pollInterval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void onFrameMetricsAvailable(
            Window window, FrameMetrics frameMetrics, int dropCountSinceLastInvocation) {
        long drawDurationsNs = frameMetrics.getMetric(FrameMetrics.DRAW_DURATION);
        // ignore values < 0; something must have gone wrong
        if (drawDurationsNs >= 0) {
            synchronized (lock) {
                // calculation copied from FrameMetricsAggregator
                int durationMs = (int) ((drawDurationsNs + NANOS_ROUNDING_VALUE) / NANOS_PER_MS);
                int oldValue = drawDurationHistogram.get(durationMs);
                drawDurationHistogram.put(durationMs, (oldValue + 1));
            }
        }
    }

    private SparseIntArray getMetrics() {
        synchronized (lock) {
            return drawDurationHistogram.clone();
        }
    }

    private SparseIntArray resetMetrics() {
        synchronized (lock) {
            SparseIntArray metrics = drawDurationHistogram;
            drawDurationHistogram = new SparseIntArray();
            return metrics;
        }
    }

    private void reportSlowRenders() {
        try {
            reportSlow(resetMetrics());
        } catch (Exception e) {
            Log.w(LOG_TAG, "Exception while processing frame metrics", e);
        }
    }

    private void reportSlow(SparseIntArray durationToCountHistogram) {
        int slowCount = 0;
        int frozenCount = 0;
        for (int i = 0; i < durationToCountHistogram.size(); i++) {
            int duration = durationToCountHistogram.keyAt(i);
            int count = durationToCountHistogram.get(duration);
            if (duration > FROZEN_THRESHOLD_MS) {
                Log.d(LOG_TAG, "* FROZEN RENDER DETECTED: " + duration + " ms." + count + " times");
                frozenCount += count;
            } else if (duration > SLOW_THRESHOLD_MS) {
                Log.d(LOG_TAG, "* Slow render detected: " + duration + " ms. " + count + " times");
                slowCount += count;
            }
        }

        Instant now = Instant.now();
        if (slowCount > 0) {
            makeSpan("slowRenders", slowCount, now);
        }
        if (frozenCount > 0) {
            makeSpan("frozenRenders", frozenCount, now);
        }
    }

    private void makeSpan(String name, int slowCount, Instant now) {
        Span span =
                tracer.spanBuilder(name)
                        .setAttribute("count", slowCount)
                        .setStartTimestamp(now)
                        .startSpan();
        span.end(now);
    }
}
