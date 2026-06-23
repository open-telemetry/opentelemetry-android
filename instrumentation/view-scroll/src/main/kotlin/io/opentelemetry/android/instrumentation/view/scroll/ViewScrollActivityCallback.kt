/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.scroll

import android.app.Activity
import io.opentelemetry.android.instrumentation.internal.InternalViewApi
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

@InternalViewApi
internal class ViewScrollActivityCallback(
    private val viewScrollEventGenerator: ViewScrollEventGenerator,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        viewScrollEventGenerator.startTracking(activity.window)
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        viewScrollEventGenerator.stopTracking()
    }
}
