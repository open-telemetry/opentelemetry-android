package com.splunk.rum;

import static androidx.core.app.FrameMetricsAggregator.DRAW_DURATION;
import static androidx.core.app.FrameMetricsAggregator.DRAW_INDEX;
import static com.splunk.rum.SplunkRum.LOG_TAG;

import android.app.Activity;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.core.app.FrameMetricsAggregator;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class SlowRenderingDetectorImpl implements SlowRenderingDetector {

    public static final int SLOW_THRESHOLD_MS = 16;
    public static final int FROZEN_THRESHOLD_MS = 700;
    private final FrameMetricsAggregator frameMetrics;
    private final ScheduledExecutorService executorService;

    private final Set<Activity> activities = new HashSet<>();
    private final Tracer tracer;
    private final Duration slowRenderPollingDuration;

    public SlowRenderingDetectorImpl(Tracer tracer, Duration slowRenderPollingDuration) {
        this(tracer, new FrameMetricsAggregator(DRAW_DURATION), Executors.newScheduledThreadPool(1), slowRenderPollingDuration);
    }

    // Exists for testing
    SlowRenderingDetectorImpl(Tracer tracer, FrameMetricsAggregator frameMetricsAggregator, ScheduledExecutorService executorService, Duration slowRenderPollingDuration) {
        this.tracer = tracer;
        this.frameMetrics = frameMetricsAggregator;
        this.executorService = executorService;
        this.slowRenderPollingDuration = slowRenderPollingDuration;
    }

    @Override
    public void add(Activity activity) {
        activities.add(activity);
        frameMetrics.add(activity);
    }

    @Override
    public void stop(Activity activity) {
        SparseIntArray[] arrays = frameMetrics.remove(activity);
        activities.remove(activity);
        if (arrays != null) {
            reportSlow(arrays[DRAW_INDEX]);
        }
    }

    @Override
    public void start() {
        executorService.scheduleAtFixedRate(this::reportSlowRenders, slowRenderPollingDuration.toMillis(), slowRenderPollingDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void reportSlowRenders() {
        try {
            SparseIntArray[] metrics = frameMetrics.reset();
            if (metrics != null) {
                reportSlow(metrics[DRAW_INDEX]);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "Exception while processing frame metrics", e);
        }
        for (Activity activity : activities) {
            frameMetrics.remove(activity);
            frameMetrics.add(activity);
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
        if(slowCount > 0){
            makeSpan("slowRenders", slowCount, now);
        }
        if(frozenCount > 0){
            makeSpan("frozenRenders", frozenCount, now);
        }
    }

    private void makeSpan(String name, int slowCount, Instant now) {
        Span span = tracer
                .spanBuilder(name)
                .setAttribute("count", slowCount)
                .setStartTimestamp(now)
                .startSpan();
        span.end(now);
    }
}
