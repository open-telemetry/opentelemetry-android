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
        val instrumentation = createInstrumentation()

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
        val instrumentation = createInstrumentation()

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

    @Test
    fun shouldShutDownSchedulerOnUninstall() {
        val (context, openTelemetryRum) = createTestDependencies()
        val instrumentation = createInstrumentation()

        instrumentation.install(context, openTelemetryRum)
        instrumentation.uninstall(context, openTelemetryRum)

        verify(exactly = 1) { scheduler.shutdownNow() }
    }

    @Test
    fun shouldNotCreateSchedulerUntilInstall() {
        var factoryCallCount = 0
        val instrumentation = AnrInstrumentation()
        instrumentation.setMainLooper(mainLooper)
        instrumentation.setSchedulerFactory {
            factoryCallCount++
            scheduler
        }

        assertThat(factoryCallCount).isEqualTo(0)

        val (context, openTelemetryRum) = createTestDependencies()
        instrumentation.install(context, openTelemetryRum)

        assertThat(factoryCallCount).isEqualTo(1)
    }

    @Test
    fun shouldGuardAgainstDoubleInstall() {
        val (context, openTelemetryRum) = createTestDependencies()
        val instrumentation = createInstrumentation()

        instrumentation.install(context, openTelemetryRum)
        instrumentation.install(context, openTelemetryRum)

        verify(exactly = 1) {
            appLifecycle.registerListener(any())
        }
    }

    @Test
    fun shouldSupportReinstallAfterUninstall() {
        val (context, openTelemetryRum) = createTestDependencies()
        val schedulers = mutableListOf<ScheduledExecutorService>()
        val instrumentation = AnrInstrumentation()
        instrumentation.setMainLooper(mainLooper)
        instrumentation.setSchedulerFactory {
            val s = mockk<ScheduledExecutorService>(relaxed = true)
            schedulers.add(s)
            s
        }

        // First install → uninstall cycle
        instrumentation.install(context, openTelemetryRum)
        instrumentation.uninstall(context, openTelemetryRum)

        assertThat(schedulers).hasSize(1)
        verify(exactly = 1) { schedulers[0].shutdownNow() }

        // Second install cycle uses a fresh scheduler
        instrumentation.install(context, openTelemetryRum)

        assertThat(schedulers).hasSize(2)
        verify(exactly = 2) {
            appLifecycle.registerListener(any())
        }
    }

    private fun createInstrumentation(): AnrInstrumentation {
        val instrumentation = AnrInstrumentation()
        instrumentation.setMainLooper(mainLooper)
        instrumentation.setSchedulerFactory { scheduler }
        return instrumentation
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
