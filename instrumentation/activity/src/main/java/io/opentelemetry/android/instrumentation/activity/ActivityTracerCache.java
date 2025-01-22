/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity;

import android.app.Activity;
import androidx.annotation.VisibleForTesting;
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer;
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker;
import io.opentelemetry.api.trace.Tracer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Encapsulates the fact that we have an ActivityTracer instance per Activity class, and provides
 * convenience methods for adding events and starting spans.
 */
public class ActivityTracerCache {

    private final Map<String, ActivityTracer> tracersByActivityClassName = new HashMap<>();

    private final Function<Activity, ActivityTracer> tracerFactory;

    public ActivityTracerCache(
            Tracer tracer,
            VisibleScreenTracker visibleScreenTracker,
            AppStartupTimer startupTimer,
            ScreenNameExtractor screenNameExtractor) {
        this(
                tracer,
                visibleScreenTracker,
                new AtomicReference<>(),
                startupTimer,
                screenNameExtractor);
    }

    @VisibleForTesting
    ActivityTracerCache(
            Tracer tracer,
            VisibleScreenTracker visibleScreenTracker,
            AtomicReference<String> initialAppActivity,
            AppStartupTimer startupTimer,
            ScreenNameExtractor screenNameExtractor) {
        this(
                activity ->
                        ActivityTracer.builder(activity)
                                .setScreenName(screenNameExtractor.extract(activity))
                                .setInitialAppActivity(initialAppActivity)
                                .setTracer(tracer)
                                .setAppStartupTimer(startupTimer)
                                .setVisibleScreenTracker(visibleScreenTracker)
                                .build());
    }

    @VisibleForTesting
    ActivityTracerCache(Function<Activity, ActivityTracer> tracerFactory) {
        this.tracerFactory = tracerFactory;
    }

    public ActivityTracer addEvent(Activity activity, String eventName) {
        return getTracer(activity).addEvent(eventName);
    }

    public ActivityTracer startSpanIfNoneInProgress(Activity activity, String spanName) {
        return getTracer(activity).startSpanIfNoneInProgress(spanName);
    }

    public ActivityTracer initiateRestartSpanIfNecessary(Activity activity) {
        boolean isMultiActivityApp = tracersByActivityClassName.size() > 1;
        return getTracer(activity).initiateRestartSpanIfNecessary(isMultiActivityApp);
    }

    public ActivityTracer startActivityCreation(Activity activity) {
        return getTracer(activity).startActivityCreation();
    }

    private ActivityTracer getTracer(Activity activity) {
        ActivityTracer activityTracer =
                tracersByActivityClassName.get(activity.getClass().getName());
        if (activityTracer == null) {
            activityTracer = tracerFactory.apply(activity);
            tracersByActivityClassName.put(activity.getClass().getName(), activityTracer);
        }
        return activityTracer;
    }
}
