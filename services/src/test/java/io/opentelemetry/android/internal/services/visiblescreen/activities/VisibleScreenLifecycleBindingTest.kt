/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.activities

import android.app.Activity
import io.mockk.confirmVerified
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import org.junit.jupiter.api.Test

internal class VisibleScreenLifecycleBindingTest {
    private val activity: Activity = mockk()

    private val visibleScreenTracker: VisibleScreenTracker = mockk(relaxed = true)

    @Test
    fun postResumed() {
        val underTest =
            VisibleScreenLifecycleBinding(visibleScreenTracker)
        underTest.onActivityPostResumed(activity)
        verify { visibleScreenTracker.activityResumed(activity) }
        confirmVerified(visibleScreenTracker)
    }

    @Test
    fun prePaused() {
        val underTest =
            VisibleScreenLifecycleBinding(visibleScreenTracker)
        underTest.onActivityPrePaused(activity)
        verify { visibleScreenTracker.activityPaused(activity) }
    }
}
