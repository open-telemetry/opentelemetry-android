/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.screenorientation

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import org.junit.Before
import org.junit.Test

class ScreenOrientationInstrumentationTest {
    private lateinit var sut: ScreenOrientationInstrumentation

    private val context = mockk<Application>(relaxed = true)
    private val openTelemetryRum = mockk<OpenTelemetryRum>(relaxed = true)

    @Before
    fun setup() {
        sut = ScreenOrientationInstrumentation()
        every { context.applicationContext } returns context
    }

    @Test
    fun `should register component callbacks on install`() {
        // when
        sut.install(context, openTelemetryRum)

        // then
        verify {
            context.registerComponentCallbacks(any<ScreenOrientationDetector>())
        }
    }

    @Test
    fun `should unregister component callbacks on uninstall`() {
        // given
        sut.install(context, openTelemetryRum)

        // when
        sut.uninstall(context, openTelemetryRum)

        // then
        verify {
            context.unregisterComponentCallbacks(any<ScreenOrientationDetector>())
        }
    }
}
