/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import android.os.Bundle
import io.mockk.mockk

internal class ActivityCallbackTestHarness(
    private val callbacks: ActivityCallbacks,
) {
    fun runAppStartupLifecycle(mainActivity: Activity) {
        // app startup lifecycle is the same as a normal activity lifecycle
        runActivityCreationLifecycle(mainActivity)
    }

    fun runActivityCreationLifecycle(activity: Activity) {
        val bundle = mockk<Bundle>()
        callbacks.onActivityPreCreated(activity, bundle)
        callbacks.onActivityCreated(activity, bundle)
        callbacks.onActivityPostCreated(activity, bundle)
        runActivityStartedLifecycle(activity)
        runActivityResumedLifecycle(activity)
    }

    fun runActivityStartedLifecycle(activity: Activity) {
        callbacks.onActivityPreStarted(activity)
        callbacks.onActivityStarted(activity)
        callbacks.onActivityPostStarted(activity)
    }

    fun runActivityPausedLifecycle(activity: Activity) {
        callbacks.onActivityPrePaused(activity)
        callbacks.onActivityPaused(activity)
        callbacks.onActivityPostPaused(activity)
    }

    fun runActivityResumedLifecycle(activity: Activity) {
        callbacks.onActivityPreResumed(activity)
        callbacks.onActivityResumed(activity)
        callbacks.onActivityPostResumed(activity)
    }

    fun runActivityStoppedFromRunningLifecycle(activity: Activity) {
        runActivityPausedLifecycle(activity)
        runActivityStoppedFromPausedLifecycle(activity)
    }

    fun runActivityStoppedFromPausedLifecycle(activity: Activity) {
        callbacks.onActivityPreStopped(activity)
        callbacks.onActivityStopped(activity)
        callbacks.onActivityPostStopped(activity)
    }

    fun runActivityDestroyedFromStoppedLifecycle(activity: Activity) {
        callbacks.onActivityPreDestroyed(activity)
        callbacks.onActivityDestroyed(activity)
        callbacks.onActivityPostDestroyed(activity)
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
