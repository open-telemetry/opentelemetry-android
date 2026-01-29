/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.startup

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.common.Clock
import java.util.concurrent.TimeUnit

internal class AppStartupTimer {
    private lateinit var startupClock: AnchoredClock
    private var firstPossibleTimestamp: Long = 0

    @Volatile
    var startupSpan: Span? = null
        private set

    // whether activity has been created
    // accessed only from UI thread
    private var uiInitStarted = false

    // whether MAX_TIME_TO_UI_INIT has been exceeded
    // accessed only from UI thread
    private var uiInitTooLate = false

    fun start(
        tracer: Tracer,
        clock: Clock,
    ): Span {
        // guard against a double-start and just return what's already in flight.
        startupSpan?.let {
            return it
        }
        startupClock = AnchoredClock(clock)
        firstPossibleTimestamp = startupClock.now()
        val appStart =
            tracer
                .spanBuilder("AppStart")
                .setStartTimestamp(firstPossibleTimestamp, TimeUnit.NANOSECONDS)
                .setAttribute(RumConstants.START_TYPE_KEY, "cold")
                .startSpan()
        this.startupSpan = appStart
        return appStart
    }

    /**
     * Creates a lifecycle listener that starts the UI init when an activity is created.
     *
     * @return a new Application.ActivityLifecycleCallbacks instance
     */
    fun createLifecycleCallback(): ActivityLifecycleCallbacks =
        object : DefaultingActivityLifecycleCallbacks {
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?,
            ) {
                startUiInit()
            }
        }

    /** Called when Activity is created.  */
    private fun startUiInit() {
        if (uiInitStarted) {
            return
        }
        uiInitStarted = true
        if (firstPossibleTimestamp + MAX_TIME_TO_UI_INIT < startupClock.now()) {
            Log.d(RumConstants.OTEL_RUM_LOG_TAG, "Max time to UI init exceeded")
            uiInitTooLate = true
            clear()
        }
    }

    fun end() {
        val overallAppStartSpan = this.startupSpan
        if (overallAppStartSpan != null && !uiInitTooLate) {
            overallAppStartSpan.end(startupClock.now(), TimeUnit.NANOSECONDS)
        }
        clear()
    }

    private fun clear() {
        this.startupSpan = null
    }

    companion object {
        // Maximum time from app start to creation of the UI. If this time is exceeded we will not
        // create the app start span. Long app startup could indicate that the app was really started in
        // background, in which case the measured startup time is misleading.
        private val MAX_TIME_TO_UI_INIT = TimeUnit.MINUTES.toNanos(1)
    }
}
