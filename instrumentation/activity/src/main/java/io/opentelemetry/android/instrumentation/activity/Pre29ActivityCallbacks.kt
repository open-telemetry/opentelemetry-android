/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import android.os.Bundle
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

internal class Pre29ActivityCallbacks(
    private val tracers: ActivityTracerCache,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        tracers.startActivityCreation(activity).addEvent("activityCreated")
    }

    override fun onActivityStarted(activity: Activity) {
        tracers.initiateRestartSpanIfNecessary(activity).addEvent("activityStarted")
    }

    override fun onActivityResumed(activity: Activity) {
        tracers
            .startSpanIfNoneInProgress(activity, "Resumed")
            .addEvent("activityResumed")
            .addPreviousScreenAttribute()
            .endSpanForActivityResumed()
    }

    override fun onActivityPaused(activity: Activity) {
        tracers
            .startSpanIfNoneInProgress(activity, "Paused")
            .addEvent("activityPaused")
            .endActiveSpan()
    }

    override fun onActivityStopped(activity: Activity) {
        tracers
            .startSpanIfNoneInProgress(activity, "Stopped")
            .addEvent("activityStopped")
            .endActiveSpan()
    }

    override fun onActivityDestroyed(activity: Activity) {
        tracers
            .startSpanIfNoneInProgress(activity, "Destroyed")
            .addEvent("activityDestroyed")
            .endActiveSpan()
    }
}
