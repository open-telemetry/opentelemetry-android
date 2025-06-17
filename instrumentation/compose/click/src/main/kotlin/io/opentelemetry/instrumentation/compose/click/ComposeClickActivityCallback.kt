/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.click

import android.app.Activity
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

internal class ComposeClickActivityCallback(
    private val composeClickEventGenerator: ComposeClickEventGenerator,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        composeClickEventGenerator.startTracking(activity.window)
    }

    override fun onActivityPaused(activity: Activity) {
        composeClickEventGenerator.stopTracking()
    }
}
