/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.thermal

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class ThermalInstrumentationTest {
    private lateinit var sut: ThermalInstrumentation

    private val context = mockk<Application>(relaxed = true)
    private val openTelemetryRum = mockk<OpenTelemetryRum>(relaxed = true)
    private val powerManager = mockk<PowerManager>(relaxed = true)

    @Before
    fun setup() {
        sut = ThermalInstrumentation()
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
    }

    @Config(sdk = [Build.VERSION_CODES.P])
    @Test
    fun `should not register thermal listener on api levels below Q`() {
        // when
        sut.install(context, openTelemetryRum)

        // then: install no-ops before it ever looks up the PowerManager.
        // (The API 29+ listener type can't be referenced under an API 28 runtime.)
        verify(exactly = 0) { context.getSystemService(any<String>()) }
    }

    @Config(sdk = [Build.VERSION_CODES.Q])
    @Test
    fun `should register thermal listener on install`() {
        // when
        sut.install(context, openTelemetryRum)

        // then
        verify { powerManager.addThermalStatusListener(any(), any<ThermalDetector>()) }
    }

    @Config(sdk = [Build.VERSION_CODES.Q])
    @Test
    fun `should remove thermal listener on uninstall`() {
        // given
        sut.install(context, openTelemetryRum)

        // when
        sut.uninstall(context, openTelemetryRum)

        // then
        verify { powerManager.removeThermalStatusListener(any<ThermalDetector>()) }
    }
}
