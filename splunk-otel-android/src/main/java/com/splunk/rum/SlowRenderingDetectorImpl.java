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

import static androidx.core.app.FrameMetricsAggregator.DRAW_DURATION;
import static androidx.core.app.FrameMetricsAggregator.DRAW_INDEX;
import static com.splunk.rum.SplunkRum.LOG_TAG;

import android.app.Activity;
import android.util.Log;
import android.util.SparseIntArray;
import androidx.core.app.FrameMetricsAggregator;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class SlowRenderingDetectorImpl implements SlowRenderingDetector {

    static final int SLOW_THRESHOLD_MS = 16;
    static final int FROZEN_THRESHOLD_MS = 700;
    private final FrameMetricsAggregator frameMetrics;
    private final ScheduledExecutorService executorService;

    private final Set<Activity> activities = new HashSet<>();
    private final Tracer tracer;
    private final Duration pollInterval;
    private final Object lock = new Object();

    SlowRenderingDetectorImpl(Tracer tracer, Duration pollInterval) {
        this(
                tracer,
                new FrameMetricsAggregator(DRAW_DURATION),
                Executors.newScheduledThreadPool(1),
                pollInterval);
    }

    // Exists for testing
    SlowRenderingDetectorImpl(
            Tracer tracer,
            FrameMetricsAggregator frameMetricsAggregator,
            ScheduledExecutorService executorService,
            Duration pollInterval) {
        this.tracer = tracer;
        this.frameMetrics = frameMetricsAggregator;
        this.executorService = executorService;
        this.pollInterval = pollInterval;
    }

    @Override
    public void add(Activity activity) {
        synchronized (lock) {
            activities.add(activity);
            frameMetrics.add(activity);
        }
    }

    @Override
    public void stop(Activity activity) {
        SparseIntArray[] arrays;
        synchronized (lock) {
            arrays = frameMetrics.remove(activity);
            activities.remove(activity);
        }
        if (arrays != null) {
            reportSlow(arrays[DRAW_INDEX]);
        }
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

    private void reportSlowRenders() {
        try {
            SparseIntArray[] metrics;
            synchronized (lock) {
                metrics = frameMetrics.reset();
            }
            if (metrics != null) {
                reportSlow(metrics[DRAW_INDEX]);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "Exception while processing frame metrics", e);
        }
        synchronized (lock) {
            try {
                for (Activity activity : activities) {
                    frameMetrics.remove(activity);
                    frameMetrics.add(activity);
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, "Exception updating observed activities", e);
            }
        }
    }

    private void reportSlow(SparseIntArray durationToCountHistogram) {
        if (durationToCountHistogram == null) {
            return;
        }
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
