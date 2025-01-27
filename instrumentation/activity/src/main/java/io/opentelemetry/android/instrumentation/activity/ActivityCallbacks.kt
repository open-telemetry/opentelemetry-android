/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import android.os.Bundle
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

class ActivityCallbacks(
    private val tracers: ActivityTracerCache,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityPreCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        tracers.startActivityCreation(activity).addEvent("activityPreCreated")
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        tracers.addEvent(activity, "activityCreated")
    }

    override fun onActivityPostCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        tracers.addEvent(activity, "activityPostCreated")
    }

    override fun onActivityPreStarted(activity: Activity) {
        tracers.initiateRestartSpanIfNecessary(activity).addEvent("activityPreStarted")
    }

    override fun onActivityStarted(activity: Activity) {
        tracers.addEvent(activity, "activityStarted")
    }

    override fun onActivityPostStarted(activity: Activity) {
        tracers.addEvent(activity, "activityPostStarted")
    }

    override fun onActivityPreResumed(activity: Activity) {
        tracers.startSpanIfNoneInProgress(activity, "Resumed").addEvent("activityPreResumed")
    }

    override fun onActivityResumed(activity: Activity) {
        tracers.addEvent(activity, "activityResumed")
    }

    override fun onActivityPostResumed(activity: Activity) {
        tracers
            .addEvent(activity, "activityPostResumed")
            .addPreviousScreenAttribute()
            .endSpanForActivityResumed()
    }

    override fun onActivityPrePaused(activity: Activity) {
        tracers.startSpanIfNoneInProgress(activity, "Paused").addEvent("activityPrePaused")
    }

    override fun onActivityPaused(activity: Activity) {
        tracers.addEvent(activity, "activityPaused")
    }

    override fun onActivityPostPaused(activity: Activity) {
        tracers.addEvent(activity, "activityPostPaused").endActiveSpan()
    }

    override fun onActivityPreStopped(activity: Activity) {
        tracers.startSpanIfNoneInProgress(activity, "Stopped").addEvent("activityPreStopped")
    }

    override fun onActivityStopped(activity: Activity) {
        tracers.addEvent(activity, "activityStopped")
    }

    override fun onActivityPostStopped(activity: Activity) {
        tracers.addEvent(activity, "activityPostStopped").endActiveSpan()
    }

    override fun onActivityPreDestroyed(activity: Activity) {
        tracers.startSpanIfNoneInProgress(activity, "Destroyed").addEvent("activityPreDestroyed")
    }

    override fun onActivityDestroyed(activity: Activity) {
        tracers.addEvent(activity, "activityDestroyed")
    }

    override fun onActivityPostDestroyed(activity: Activity) {
        tracers.addEvent(activity, "activityPostDestroyed").endActiveSpan()
    }
}
