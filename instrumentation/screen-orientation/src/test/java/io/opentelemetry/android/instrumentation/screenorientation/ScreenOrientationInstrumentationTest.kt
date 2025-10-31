/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.screenorientation

import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.instrumentation.InstallationContext
import org.junit.Before
import org.junit.Test

class ScreenOrientationInstrumentationTest {
    private lateinit var sut: ScreenOrientationInstrumentation

    private val installationContext = mockk<InstallationContext>(relaxed = true)

    @Before
    fun setup() {
        sut = ScreenOrientationInstrumentation()
    }

    @Test
    fun `should register component callbacks on install`() {
        // when
        sut.install(installationContext)

        // then
        verify {
            installationContext.context.applicationContext.registerComponentCallbacks(any<ScreenOrientationDetector>())
        }
    }

    @Test
    fun `should unregister component callbacks on uninstall`() {
        // given
        sut.install(installationContext)

        // when
        sut.uninstall(installationContext)

        // then
        verify {
            installationContext.context.applicationContext.unregisterComponentCallbacks(any<ScreenOrientationDetector>())
        }
    }
}
