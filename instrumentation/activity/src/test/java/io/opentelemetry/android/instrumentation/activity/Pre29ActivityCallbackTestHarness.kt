/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import android.os.Bundle
import io.mockk.mockk

internal class Pre29ActivityCallbackTestHarness(
    private val callbacks: Pre29ActivityCallbacks,
) {
    fun runAppStartupLifecycle(mainActivity: Activity) {
        // app startup lifecycle is the same as a normal activity lifecycle
        runActivityCreationLifecycle(mainActivity)
    }

    fun runActivityCreationLifecycle(activity: Activity) {
        val bundle = mockk<Bundle>()
        callbacks.onActivityCreated(activity, bundle)
        runActivityStartedLifecycle(activity)
        runActivityResumedLifecycle(activity)
    }

    fun runActivityStartedLifecycle(activity: Activity) {
        callbacks.onActivityStarted(activity)
    }

    fun runActivityPausedLifecycle(activity: Activity) {
        callbacks.onActivityPaused(activity)
    }

    fun runActivityResumedLifecycle(activity: Activity) {
        callbacks.onActivityResumed(activity)
    }

    fun runActivityStoppedFromRunningLifecycle(activity: Activity) {
        runActivityPausedLifecycle(activity)
        runActivityStoppedFromPausedLifecycle(activity)
    }

    fun runActivityStoppedFromPausedLifecycle(activity: Activity) {
        callbacks.onActivityStopped(activity)
    }

    fun runActivityDestroyedFromStoppedLifecycle(activity: Activity) {
        callbacks.onActivityDestroyed(activity)
    }

    fun runActivityDestroyedFromPausedLifecycle(activity: Activity) {
        runActivityStoppedFromPausedLifecycle(activity)
        runActivityDestroyedFromStoppedLifecycle(activity)
    }

    fun runActivityDestroyedFromRunningLifecycle(activity: Activity) {
        runActivityStoppedFromRunningLifecycle(activity)
        runActivityDestroyedFromStoppedLifecycle(activity)
    }

    fun runActivityRestartedLifecycle(activity: Activity) {
        runActivityStartedLifecycle(activity)
        runActivityResumedLifecycle(activity)
    }
}
