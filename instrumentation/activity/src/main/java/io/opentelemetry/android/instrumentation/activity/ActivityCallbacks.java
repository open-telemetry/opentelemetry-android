/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks;

public class ActivityCallbacks implements DefaultingActivityLifecycleCallbacks {

    private final ActivityTracerCache tracers;

    public ActivityCallbacks(ActivityTracerCache tracers) {
        this.tracers = tracers;
    }

    @Override
    public void onActivityPreCreated(
            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        tracers.startActivityCreation(activity).addEvent("activityPreCreated");
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        tracers.addEvent(activity, "activityCreated");
    }

    @Override
    public void onActivityPostCreated(
            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        tracers.addEvent(activity, "activityPostCreated");
    }

    @Override
    public void onActivityPreStarted(@NonNull Activity activity) {
        tracers.initiateRestartSpanIfNecessary(activity).addEvent("activityPreStarted");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityStarted");
    }

    @Override
    public void onActivityPostStarted(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPostStarted");
    }

    @Override
    public void onActivityPreResumed(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Resumed").addEvent("activityPreResumed");
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityResumed");
    }

    @Override
    public void onActivityPostResumed(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPostResumed")
                .addPreviousScreenAttribute()
                .endSpanForActivityResumed();
    }

    @Override
    public void onActivityPrePaused(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Paused").addEvent("activityPrePaused");
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPaused");
    }

    @Override
    public void onActivityPostPaused(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPostPaused").endActiveSpan();
    }

    @Override
    public void onActivityPreStopped(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Stopped").addEvent("activityPreStopped");
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityStopped");
    }

    @Override
    public void onActivityPostStopped(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPostStopped").endActiveSpan();
    }

    @Override
    public void onActivityPreDestroyed(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Destroyed").addEvent("activityPreDestroyed");
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityDestroyed");
    }

    @Override
    public void onActivityPostDestroyed(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPostDestroyed").endActiveSpan();
    }
}
