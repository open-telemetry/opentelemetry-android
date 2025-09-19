/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.activities

import android.app.Activity
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class Pre29VisibleScreenLifecycleBindingTest {
    @Mock
    private lateinit var activity: Activity

    @Mock
    private lateinit var visibleScreenTracker: VisibleScreenTracker

    @Test
    fun postResumed() {
        val underTest =
            Pre29VisibleScreenLifecycleBinding(visibleScreenTracker)
        underTest.onActivityResumed(activity)
        Mockito.verify(visibleScreenTracker).activityResumed(activity)
        Mockito.verifyNoMoreInteractions(visibleScreenTracker)
    }

    @Test
    fun prePaused() {
        val underTest =
            Pre29VisibleScreenLifecycleBinding(visibleScreenTracker)
        underTest.onActivityPaused(activity)
        Mockito.verify(visibleScreenTracker).activityPaused(activity)
        Mockito.verifyNoMoreInteractions(visibleScreenTracker)
    }
}
