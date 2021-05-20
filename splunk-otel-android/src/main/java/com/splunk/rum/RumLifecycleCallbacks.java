package com.splunk.rum;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.api.trace.Tracer;

class RumLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final NoOpTracer NO_OP_TRACER = new NoOpTracer();

    private final Map<String, ActivityTracer> tracersByActivityClassName = new HashMap<>();
    private final AtomicBoolean appStartupComplete = new AtomicBoolean();
    private final Tracer tracer;

    RumLifecycleCallbacks(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        getOrCreateTracer(activity)
                .startActivityCreation()
                .addEvent("activityPreCreated");
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        addEvent(activity, "activityCreated");
    }

    @Override
    public void onActivityPostCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        addEvent(activity, "activityPostCreated");
    }

    @Override
    public void onActivityPreStarted(@NonNull Activity activity) {
        getOrCreateTracer(activity)
                .startSpanIfNoneInProgress("Restarted")
                .addEvent("activityPreStarted");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        addEvent(activity, "activityStarted");
    }

    @Override
    public void onActivityPostStarted(@NonNull Activity activity) {
        addEvent(activity, "activityPostStarted");
    }

    @Override
    public void onActivityPreResumed(@NonNull Activity activity) {
        getOrCreateTracer(activity)
                .startSpanIfNoneInProgress("Resumed")
                .addEvent("activityPreResumed");
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        addEvent(activity, "activityResumed");
    }

    @Override
    public void onActivityPostResumed(@NonNull Activity activity) {
        getActivityTracer(activity)
                .addEvent("activityPostResumed")
                .endSpanForActivityResumed();
    }

    @Override
    public void onActivityPrePaused(@NonNull Activity activity) {
        getOrCreateTracer(activity)
                .startSpanIfNoneInProgress("Paused")
                .addEvent("activityPrePaused");
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        addEvent(activity, "activityPaused");
    }

    @Override
    public void onActivityPostPaused(@NonNull Activity activity) {
        getActivityTracer(activity).addEvent("activityPostPaused").endActiveSpan();
    }

    @Override
    public void onActivityPreStopped(@NonNull Activity activity) {
        getOrCreateTracer(activity)
                .startSpanIfNoneInProgress("Stopped")
                .addEvent("activityPreStopped");
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        addEvent(activity, "activityStopped");
    }

    @Override
    public void onActivityPostStopped(@NonNull Activity activity) {
        getActivityTracer(activity).addEvent("activityPostStopped").endActiveSpan();
    }

    @Override
    public void onActivityPreSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityPostSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityPreDestroyed(@NonNull Activity activity) {
        getOrCreateTracer(activity)
                .startSpanIfNoneInProgress("Destroyed")
                .addEvent("activityPreDestroyed");
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        addEvent(activity, "activityDestroyed");
    }

    @Override
    public void onActivityPostDestroyed(@NonNull Activity activity) {
        getActivityTracer(activity).addEvent("activityPostDestroyed").endActiveSpan();
    }

    private void addEvent(@NonNull Activity activity, String eventName) {
        getActivityTracer(activity).addEvent(eventName);
    }

    private TrackableTracer getOrCreateTracer(Activity activity) {
        ActivityTracer activityTracer = tracersByActivityClassName.get(activity.getClass().getName());
        if (activityTracer == null) {
            activityTracer = new ActivityTracer(activity, appStartupComplete, tracer);
            tracersByActivityClassName.put(activity.getClass().getName(), activityTracer);
        }
        return activityTracer;
    }

    private TrackableTracer getActivityTracer(@NonNull Activity activity) {
        ActivityTracer activityTracer = tracersByActivityClassName.get(activity.getClass().getName());
        if (activityTracer == null) {
            return NO_OP_TRACER;
        }
        return activityTracer;
    }

    private static class NoOpTracer implements TrackableTracer {

        @Override
        public TrackableTracer startSpanIfNoneInProgress(String action) {
            return this;
        }

        @Override
        public TrackableTracer startActivityCreation() {
            return this;
        }

        @Override
        public void endSpanForActivityResumed() {
        }

        @Override
        public void endActiveSpan() {
        }

        @Override
        public TrackableTracer addEvent(String eventName) {
            return this;
        }
    }
}
