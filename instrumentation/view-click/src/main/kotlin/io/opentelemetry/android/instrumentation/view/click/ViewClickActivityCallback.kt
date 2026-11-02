/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.app.Activity
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

internal class ViewClickActivityCallback(
    private val viewClickEventGenerator: ViewClickEventGenerator,
) : DefaultingActivityLifecycleCallbacks {
    @Volatile
    var isActive: Boolean = true

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (!isActive) return
        viewClickEventGenerator.startTracking(activity.window)
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        if (!isActive) return
        viewClickEventGenerator.stopTracking()
    }
}
