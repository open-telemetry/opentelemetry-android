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

public class Pre29ActivityCallbacks implements DefaultingActivityLifecycleCallbacks {
    private final ActivityTracerCache tracers;

    public Pre29ActivityCallbacks(ActivityTracerCache tracers) {
        this.tracers = tracers;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        tracers.startActivityCreation(activity).addEvent("activityCreated");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        tracers.initiateRestartSpanIfNecessary(activity).addEvent("activityStarted");
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Resumed")
                .addEvent("activityResumed")
                .addPreviousScreenAttribute()
                .endSpanForActivityResumed();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Paused")
                .addEvent("activityPaused")
                .endActiveSpan();
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Stopped")
                .addEvent("activityStopped")
                .endActiveSpan();
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Destroyed")
                .addEvent("activityDestroyed")
                .endActiveSpan();
    }
}
