/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.click

import android.app.Activity
import android.view.Window
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ComposeClickActivityCallbackTest {
    lateinit var composeClickActivityCallback: ComposeClickActivityCallback

    @MockK
    lateinit var composeClickEventGenerator: ComposeClickEventGenerator

    @MockK
    lateinit var activity: Activity

    @MockK
    lateinit var window: Window

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        composeClickActivityCallback = ComposeClickActivityCallback(composeClickEventGenerator)
    }

    @Test
    fun `verify that call is delegated to startTracking`() {
        every { composeClickEventGenerator.startTracking(any()) } returns Unit
        every { activity.window } returns window

        composeClickActivityCallback.onActivityResumed(activity)
        verify { composeClickEventGenerator.startTracking(any()) }
    }

    @Test
    fun `verify that call is delegated to stopTracking`() {
        every { composeClickEventGenerator.stopTracking() } returns Unit
        composeClickActivityCallback.onActivityPaused(activity)

        verify { composeClickEventGenerator.stopTracking() }
    }
}
