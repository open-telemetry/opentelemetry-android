/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity;

import android.app.Activity;
import androidx.annotation.NonNull;
import io.opentelemetry.android.DefaultingActivityLifecycleCallbacks;

/**
 * An ActivityLifecycleCallbacks that is responsible for telling the VisibleScreenTracker when an
 * activity has been resumed and when an activity has been paused. It's just a glue class.
 */
public class VisibleScreenLifecycleBinding implements DefaultingActivityLifecycleCallbacks {

    private final VisibleScreenTracker visibleScreenTracker;

    public VisibleScreenLifecycleBinding(VisibleScreenTracker visibleScreenTracker) {
        this.visibleScreenTracker = visibleScreenTracker;
    }

    @Override
    public void onActivityPostResumed(@NonNull Activity activity) {
        visibleScreenTracker.activityResumed(activity);
    }

    @Override
    public void onActivityPrePaused(@NonNull Activity activity) {
        visibleScreenTracker.activityPaused(activity);
    }
}
