/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.activities;

import android.app.Activity;
import androidx.annotation.NonNull;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService;

/**
 * An ActivityLifecycleCallbacks that is responsible for telling the VisibleScreenTracker when an
 * activity has been resumed and when an activity has been paused. It's just a glue class designed
 * for API level before 29.
 */
public class Pre29VisibleScreenLifecycleBinding implements DefaultingActivityLifecycleCallbacks {

    private final VisibleScreenService visibleScreenService;

    public Pre29VisibleScreenLifecycleBinding(VisibleScreenService visibleScreenService) {
        this.visibleScreenService = visibleScreenService;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        visibleScreenService.activityResumed(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        visibleScreenService.activityPaused(activity);
    }
}
