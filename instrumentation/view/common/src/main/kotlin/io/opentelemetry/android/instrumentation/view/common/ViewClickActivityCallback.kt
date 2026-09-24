/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.common

import android.app.Activity
import android.view.Window
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

class ViewClickActivityCallback(
    private val toggleableTracker: ToggleableTracker,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        toggleableTracker.startTracking(activity.window)
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        toggleableTracker.stopTracking()
    }
}

interface ToggleableTracker {
    fun startTracking(window: Window)

    fun stopTracking()
}
