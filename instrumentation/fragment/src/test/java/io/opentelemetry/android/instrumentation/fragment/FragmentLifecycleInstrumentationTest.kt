/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment

import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.internal.services.ServicesFactory
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FragmentLifecycleInstrumentationTest {
    private lateinit var sut: FragmentLifecycleInstrumentation

    private val application = mockk<Application>(relaxed = true)
    private val openTelemetryRum = mockk<OpenTelemetryRum>()
    private val openTelemetry = mockk<OpenTelemetry>()
    private val tracer = mockk<Tracer>(relaxed = true)
    private val services = mockk<ServicesFactory>()
    private val visibleScreenTracker = mockk<VisibleScreenTracker>(relaxed = true)

    @BeforeEach
    fun setUp() {
        sut = FragmentLifecycleInstrumentation()
        every { openTelemetryRum.openTelemetry } returns openTelemetry
        every { openTelemetry.getTracer(any()) } returns tracer
        every { services.visibleScreenTracker } returns visibleScreenTracker
        sut.configureServices(services)
    }

    @Test
    fun `install without services fails before registering lifecycle callbacks`() {
        val instrumentation = FragmentLifecycleInstrumentation()

        assertThatIllegalStateException()
            .isThrownBy { instrumentation.install(application, openTelemetryRum) }
            .withMessage("Services must be configured before installation")

        verify(exactly = 0) { application.registerActivityLifecycleCallbacks(any()) }
    }

    @Test
    fun `should register activity lifecycle callbacks on install`() {
        val callbacksSlot = slot<ActivityLifecycleCallbacks>()

        sut.install(application, openTelemetryRum)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbacksSlot))
        }
    }

    @Test
    fun `should unregister activity lifecycle callbacks on uninstall`() {
        val callbacksSlot = slot<ActivityLifecycleCallbacks>()
        sut.install(application, openTelemetryRum)
        verify {
            application.registerActivityLifecycleCallbacks(capture(callbacksSlot))
        }

        sut.uninstall(application, openTelemetryRum)

        verify {
            application.unregisterActivityLifecycleCallbacks(callbacksSlot.captured)
        }
    }

    @Test
    fun `should be a safe no-op when uninstall is called before install`() {
        sut.uninstall(application, openTelemetryRum)

        verify(exactly = 0) {
            application.unregisterActivityLifecycleCallbacks(any())
        }
    }

    @Test
    fun `repeated uninstall is a safe no-op and unregisters only once`() {
        sut.install(application, openTelemetryRum)

        sut.uninstall(application, openTelemetryRum)
        sut.uninstall(application, openTelemetryRum)

        verify(exactly = 1) {
            application.unregisterActivityLifecycleCallbacks(any())
        }
    }

    @Test
    fun `non-Application context is a safe no-op on install and uninstall`() {
        val nonAppContext = mockk<Context>()

        sut.install(nonAppContext, openTelemetryRum)
        sut.uninstall(nonAppContext, openTelemetryRum)

        verify(exactly = 0) {
            application.registerActivityLifecycleCallbacks(any())
            application.unregisterActivityLifecycleCallbacks(any())
            openTelemetryRum.openTelemetry
        }
    }
}
