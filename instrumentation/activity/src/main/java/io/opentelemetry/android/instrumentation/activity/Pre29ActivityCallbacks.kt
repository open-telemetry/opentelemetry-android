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

internal class Pre29ActivityCallbacks(
    private val tracers: ActivityTracerCache,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        guarded("onActivityCreated") {
            tracers.startActivityCreation(activity).addEvent("activityCreated")
        }
    }

    override fun onActivityStarted(activity: Activity) {
        guarded("onActivityStarted") {
            tracers.initiateRestartSpanIfNecessary(activity).addEvent("activityStarted")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        guarded("onActivityResumed") {
            tracers
                .startSpanIfNoneInProgress(activity, "Resumed")
                .addEvent("activityResumed")
                .addPreviousScreenAttribute()
                .endSpanForActivityResumed()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        guarded("onActivityPaused") {
            tracers
                .startSpanIfNoneInProgress(activity, "Paused")
                .addEvent("activityPaused")
                .endActiveSpan()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        guarded("onActivityStopped") {
            tracers
                .startSpanIfNoneInProgress(activity, "Stopped")
                .addEvent("activityStopped")
                .endActiveSpan()
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        guarded("onActivityDestroyed") {
            tracers
                .startSpanIfNoneInProgress(activity, "Destroyed")
                .addEvent("activityDestroyed")
                .endActiveSpan()
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
