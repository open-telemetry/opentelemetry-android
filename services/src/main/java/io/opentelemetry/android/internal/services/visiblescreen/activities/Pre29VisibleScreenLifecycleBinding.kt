/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.activities

import android.app.Activity
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker

/**
 * An ActivityLifecycleCallbacks that is responsible for telling the VisibleScreenTracker when an
 * activity has been resumed and when an activity has been paused. It's just a glue class designed
 * for API level before 29.
 */
class Pre29VisibleScreenLifecycleBinding(
    private val visibleScreenTracker: VisibleScreenTracker,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        visibleScreenTracker.activityResumed(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        visibleScreenTracker.activityPaused(activity)
    }
}
