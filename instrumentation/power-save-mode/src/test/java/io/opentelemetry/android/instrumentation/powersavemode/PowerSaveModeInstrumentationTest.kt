/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.powersavemode

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PowerSaveModeInstrumentationTest {
    private lateinit var sut: PowerSaveModeInstrumentation

    private val context = mockk<Application>(relaxed = true)
    private val openTelemetryRum = mockk<OpenTelemetryRum>(relaxed = true)
    private val powerManager = mockk<PowerManager>(relaxed = true)

    @Before
    fun setup() {
        sut = PowerSaveModeInstrumentation()
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ACTION_POWER_SAVE_MODE_CHANGED is a protected system broadcast, so the production code
    // registers without an exported flag on purpose. Lint can't inspect the IntentFilter through
    // the mockk matcher here, so it reports a false positive on this verification.
    @Suppress("UnspecifiedRegisterReceiverFlag")
    @Test
    fun `should register a power save mode receiver on install`() {
        // when
        sut.install(context, openTelemetryRum)

        // then
        verify { context.registerReceiver(any<PowerSaveModeDetector>(), any()) }
    }

    @Test
    fun `should unregister the receiver on uninstall`() {
        // given
        sut.install(context, openTelemetryRum)

        // when
        sut.uninstall(context, openTelemetryRum)

        // then
        verify { context.unregisterReceiver(any<PowerSaveModeDetector>()) }
    }

    @Test
    fun `should be a safe no-op when uninstall is called before install`() {
        // when
        sut.uninstall(context, openTelemetryRum)

        // then
        verify(exactly = 0) { context.unregisterReceiver(any<PowerSaveModeDetector>()) }
    }
}
