/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.activities

import android.app.Activity
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class Pre29VisibleScreenLifecycleBindingTest {
    @MockK
    private lateinit var activity: Activity

    @RelaxedMockK
    private lateinit var visibleScreenTracker: VisibleScreenTracker

    @Test
    fun postResumed() {
        val underTest =
            Pre29VisibleScreenLifecycleBinding(visibleScreenTracker)
        underTest.onActivityResumed(activity)
        verify { visibleScreenTracker.activityResumed(activity) }
        confirmVerified(visibleScreenTracker)
    }

    @Test
    fun prePaused() {
        val underTest =
            Pre29VisibleScreenLifecycleBinding(visibleScreenTracker)
        underTest.onActivityPaused(activity)
        verify { visibleScreenTracker.activityPaused(activity) }
        confirmVerified(visibleScreenTracker)
    }
}
