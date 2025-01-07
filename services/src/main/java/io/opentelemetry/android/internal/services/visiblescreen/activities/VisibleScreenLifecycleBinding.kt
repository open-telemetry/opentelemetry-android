/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.activities

import android.app.Activity
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService

/**
 * An ActivityLifecycleCallbacks that is responsible for telling the VisibleScreenTracker when an
 * activity has been resumed and when an activity has been paused. It's just a glue class.
 */
class VisibleScreenLifecycleBinding(
    private val visibleScreenService: VisibleScreenService,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityPostResumed(activity: Activity) {
        visibleScreenService.activityResumed(activity)
    }

    override fun onActivityPrePaused(activity: Activity) {
        visibleScreenService.activityPaused(activity)
    }
}
