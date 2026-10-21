/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import android.app.Application
import android.os.Looper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.ScheduledExecutorService

@ExtendWith(MockKExtension::class)
internal class AnrInstrumentationTest {
    @MockK
    private lateinit var mainLooper: Looper

    @RelaxedMockK
    private lateinit var scheduler: ScheduledExecutorService

    @RelaxedMockK
    private lateinit var appLifecycle: AppLifecycle

    @BeforeEach
    fun setUp() {
        every { mainLooper.thread } returns Thread()
    }

    @Test
    fun shouldInstallAndUninstallCleanly() {
        val (context, openTelemetryRum) = createTestDependencies()
        val instrumentation = AnrInstrumentation()
        instrumentation.setMainLooper(mainLooper)
        instrumentation.setScheduler(scheduler)

        val listenerSlot = slot<ApplicationStateListener>()

        instrumentation.install(context, openTelemetryRum)

        verify(exactly = 1) {
            appLifecycle.registerListener(capture(listenerSlot))
        }

        instrumentation.uninstall(context, openTelemetryRum)

        verify(exactly = 1) {
            appLifecycle.unregisterListener(listenerSlot.captured)
        }
    }

    @Test
    fun shouldHandleUninstallBeforeInstall() {
        val (context, openTelemetryRum) = createTestDependencies()
        val instrumentation = AnrInstrumentation()

        instrumentation.uninstall(context, openTelemetryRum)

        verify(exactly = 0) {
            appLifecycle.unregisterListener(any())
        }
    }

    @Test
    fun shouldExposeCorrectName() {
        val instrumentation = AnrInstrumentation()
        assertThat(instrumentation.name).isEqualTo("anr")
    }

    private fun createTestDependencies(): Pair<Application, OpenTelemetryRum> {
        val app = mockk<Application>()
        val services = mockk<Services>()
        every { services.appLifecycle } returns appLifecycle
        Services.set(services)

        val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder().build()
        val openTelemetryRum = mockk<OpenTelemetryRum>()
        every { openTelemetryRum.openTelemetry } returns openTelemetry

        return Pair(app, openTelemetryRum)
    }
}
