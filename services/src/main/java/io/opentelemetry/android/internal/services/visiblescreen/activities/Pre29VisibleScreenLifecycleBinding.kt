/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.activities

import android.app.Activity
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService

/**
 * An ActivityLifecycleCallbacks that is responsible for telling the VisibleScreenTracker when an
 * activity has been resumed and when an activity has been paused. It's just a glue class designed
 * for API level before 29.
 */
class Pre29VisibleScreenLifecycleBinding(
    private val visibleScreenService: VisibleScreenService,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        visibleScreenService.activityResumed(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        visibleScreenService.activityPaused(activity)
    }
}
