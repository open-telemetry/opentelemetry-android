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
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class ThermalInstrumentationTest {
    private lateinit var sut: ThermalInstrumentation

    private val context = mockk<Application>(relaxed = true)
    private val openTelemetryRum = mockk<OpenTelemetryRum>(relaxed = true)
    private val powerManager = mockk<PowerManager>(relaxed = true)
    private val executor = mockk<ExecutorService>(relaxed = true)

    @Before
    fun setup() {
        sut = ThermalInstrumentation()
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        mockkStatic(Executors::class)
        every { Executors.newSingleThreadExecutor() } returns executor
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Config(sdk = [Build.VERSION_CODES.P])
    @Test
    fun `should not touch PowerManager on api levels below Q`() {
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
    fun `should remove thermal listener and shut down executor on uninstall`() {
        // given
        sut.install(context, openTelemetryRum)

        // when
        sut.uninstall(context, openTelemetryRum)

        // then
        verify { powerManager.removeThermalStatusListener(any<ThermalDetector>()) }
        verify { executor.shutdown() }
    }

    @Config(sdk = [Build.VERSION_CODES.Q])
    @Test
    fun `should be a safe no-op when uninstall is called before install`() {
        // when
        sut.uninstall(context, openTelemetryRum)

        // then
        verify(exactly = 0) { powerManager.removeThermalStatusListener(any<ThermalDetector>()) }
        verify(exactly = 0) { executor.shutdown() }
    }
}
