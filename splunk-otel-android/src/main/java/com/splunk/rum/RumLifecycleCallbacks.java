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
    private final Map<String, ActivityTracer> tracersByActivityClassName = new HashMap<>();

    private final AtomicBoolean appStartupComplete = new AtomicBoolean();

    private final Tracer tracer;

    RumLifecycleCallbacks(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        ActivityTracer activityTracer = getActivityTracer(activity);
        if (activityTracer == null) {
            activityTracer = new ActivityTracer(activity, appStartupComplete, tracer);
            tracersByActivityClassName.put(activity.getClass().getName(), activityTracer);
        }
        activityTracer.startActivityCreation();
        activityTracer.addEvent("activityPreCreated");
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
        addEvent(activity, "activityPreStarted");
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
        addEvent(activity, "activityPreResumed");
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        addEvent(activity, "activityResumed");
    }

    @Override
    public void onActivityPostResumed(@NonNull Activity activity) {
        addEvent(activity, "activityPostResumed");
        ActivityTracer activityTracer = getActivityTracer(activity);
        if (activityTracer != null) {
            activityTracer.endActivityCreation();
        }
    }

    @Override
    public void onActivityPrePaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPostPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPreStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPostStopped(@NonNull Activity activity) {

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

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPostDestroyed(@NonNull Activity activity) {

    }

    private void addEvent(@NonNull Activity activity, String eventName) {
        ActivityTracer activityTracer = getActivityTracer(activity);
        if (activityTracer != null) {
            activityTracer.addEvent(eventName);
        }
    }

    private ActivityTracer getActivityTracer(@NonNull Activity activity) {
        return tracersByActivityClassName.get(activity.getClass().getName());
    }

}
