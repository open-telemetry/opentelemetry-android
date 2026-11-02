/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.screenorientation

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
        val detectorSlot = slot<ScreenOrientationDetector>()
        sut.install(context, openTelemetryRum)
        verify {
            context.registerComponentCallbacks(capture(detectorSlot))
        }

        // when
        sut.uninstall(context, openTelemetryRum)

        // then
        verify {
            context.unregisterComponentCallbacks(detectorSlot.captured)
        }
    }

    @Test
    fun `should be a safe no-op when uninstall is called before install`() {
        // when
        sut.uninstall(context, openTelemetryRum)

        // then
        verify(exactly = 0) {
            context.unregisterComponentCallbacks(any())
        }
    }

    @Test
    fun `repeated uninstall is a safe no-op and unregisters only once`() {
        // given
        sut.install(context, openTelemetryRum)

        // when
        sut.uninstall(context, openTelemetryRum)
        sut.uninstall(context, openTelemetryRum)

        // then
        verify(exactly = 1) {
            context.unregisterComponentCallbacks(any<ScreenOrientationDetector>())
        }
    }
}
