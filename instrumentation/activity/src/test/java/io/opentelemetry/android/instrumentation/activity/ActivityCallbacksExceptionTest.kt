/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityCallbacksExceptionTest {
    private val activity = mockk<Activity>()

    private fun throwingTracers(): ActivityTracerCache {
        val tracers = mockk<ActivityTracerCache>()
        every { tracers.startActivityCreation(any()) } throws IllegalStateException("boom")
        every { tracers.initiateRestartSpanIfNecessary(any()) } throws IllegalStateException("boom")
        every { tracers.startSpanIfNoneInProgress(any(), any()) } throws IllegalStateException("boom")
        every { tracers.addEvent(any(), any()) } throws IllegalStateException("boom")
        return tracers
    }

    @Test
    fun activityCallbacks_tracerThrows_doesNotPropagate() {
        val tracers = throwingTracers()
        val harness = ActivityCallbackTestHarness(ActivityCallbacks(tracers))

        harness.runAppStartupLifecycle(activity)
        harness.runActivityPausedLifecycle(activity)
        harness.runActivityStoppedFromPausedLifecycle(activity)
        harness.runActivityDestroyedFromStoppedLifecycle(activity)
        harness.runActivityRestartedLifecycle(activity)

        verify { tracers.startActivityCreation(activity) }
    }

    @Test
    fun pre29ActivityCallbacks_tracerThrows_doesNotPropagate() {
        val tracers = throwingTracers()
        val harness = Pre29ActivityCallbackTestHarness(Pre29ActivityCallbacks(tracers))

        harness.runAppStartupLifecycle(activity)
        harness.runActivityPausedLifecycle(activity)
        harness.runActivityStoppedFromPausedLifecycle(activity)
        harness.runActivityDestroyedFromStoppedLifecycle(activity)
        harness.runActivityRestartedLifecycle(activity)

        verify { tracers.startActivityCreation(activity) }
    }
}
