/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

internal class ActivityCallbacks(
    private val tracers: ActivityTracerCache,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityPreCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        guarded("onActivityPreCreated") {
            tracers.startActivityCreation(activity).addEvent("activityPreCreated")
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        guarded("onActivityCreated") {
            tracers.addEvent(activity, "activityCreated")
        }
    }

    override fun onActivityPostCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        guarded("onActivityPostCreated") {
            tracers.addEvent(activity, "activityPostCreated")
        }
    }

    override fun onActivityPreStarted(activity: Activity) {
        guarded("onActivityPreStarted") {
            tracers.initiateRestartSpanIfNecessary(activity).addEvent("activityPreStarted")
        }
    }

    override fun onActivityStarted(activity: Activity) {
        guarded("onActivityStarted") {
            tracers.addEvent(activity, "activityStarted")
        }
    }

    override fun onActivityPostStarted(activity: Activity) {
        guarded("onActivityPostStarted") {
            tracers.addEvent(activity, "activityPostStarted")
        }
    }

    override fun onActivityPreResumed(activity: Activity) {
        guarded("onActivityPreResumed") {
            tracers.startSpanIfNoneInProgress(activity, "Resumed").addEvent("activityPreResumed")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        guarded("onActivityResumed") {
            tracers.addEvent(activity, "activityResumed")
        }
    }

    override fun onActivityPostResumed(activity: Activity) {
        guarded("onActivityPostResumed") {
            tracers
                .addEvent(activity, "activityPostResumed")
                .addPreviousScreenAttribute()
                .endSpanForActivityResumed()
        }
    }

    override fun onActivityPrePaused(activity: Activity) {
        guarded("onActivityPrePaused") {
            tracers.startSpanIfNoneInProgress(activity, "Paused").addEvent("activityPrePaused")
        }
    }

    override fun onActivityPaused(activity: Activity) {
        guarded("onActivityPaused") {
            tracers.addEvent(activity, "activityPaused")
        }
    }

    override fun onActivityPostPaused(activity: Activity) {
        guarded("onActivityPostPaused") {
            tracers.addEvent(activity, "activityPostPaused").endActiveSpan()
        }
    }

    override fun onActivityPreStopped(activity: Activity) {
        guarded("onActivityPreStopped") {
            tracers.startSpanIfNoneInProgress(activity, "Stopped").addEvent("activityPreStopped")
        }
    }

    override fun onActivityStopped(activity: Activity) {
        guarded("onActivityStopped") {
            tracers.addEvent(activity, "activityStopped")
        }
    }

    override fun onActivityPostStopped(activity: Activity) {
        guarded("onActivityPostStopped") {
            tracers.addEvent(activity, "activityPostStopped").endActiveSpan()
        }
    }

    override fun onActivityPreDestroyed(activity: Activity) {
        guarded("onActivityPreDestroyed") {
            tracers.startSpanIfNoneInProgress(activity, "Destroyed").addEvent("activityPreDestroyed")
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        guarded("onActivityDestroyed") {
            tracers.addEvent(activity, "activityDestroyed")
        }
    }

    override fun onActivityPostDestroyed(activity: Activity) {
        guarded("onActivityPostDestroyed") {
            tracers.addEvent(activity, "activityPostDestroyed").endActiveSpan()
        }
    }

    private inline fun guarded(
        callback: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (e: Exception) {
            // runs inside the host app's lifecycle dispatch on the main thread, so it must not throw
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Exception in activity lifecycle callback $callback", e)
        }
    }
}
