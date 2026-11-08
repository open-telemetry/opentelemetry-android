/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.android.semconv.StartAttributes.START_TYPE_KEY
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.Clock
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

@RunWith(AndroidJUnit4::class)
class ActivityLifecycleInstrumentationTest {
    private lateinit var activityLifecycleInstrumentation: ActivityLifecycleInstrumentation
    private lateinit var application: Application
    private lateinit var openTelemetry: OpenTelemetrySdk
    private lateinit var sessionProvider: SessionProvider
    private lateinit var services: Services

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        openTelemetry = mockk()
        activityLifecycleInstrumentation = ActivityLifecycleInstrumentation()
        services = mockk()
        sessionProvider = mockk()
        every { services.visibleScreenTracker }.returns(mockk<VisibleScreenTracker>())
        Services.set(services)
    }

    @After
    fun tearDown() {
        Services.set(null)
    }

    @Test
    fun `Installing instrumentation starts AppStartupTimer`() {
        val tracer: Tracer = mockk()
        val startupSpanBuilder: SpanBuilder = mockk()
        val startupSpan: Span = mockk()

        every { openTelemetry.getTracer("io.opentelemetry.lifecycle") }.returns(tracer)
        every { tracer.spanBuilder("AppStart") }.returns(startupSpanBuilder)
        every { startupSpanBuilder.setStartTimestamp(any(), any()) }.returns(startupSpanBuilder)
        every { startupSpanBuilder.setAttribute(START_TYPE_KEY, "cold") }.returns(
            startupSpanBuilder,
        )
        every { startupSpanBuilder.startSpan() }.returns(startupSpan)

        val openTelemetryRum = createOpenTelemetryRum()
        activityLifecycleInstrumentation.install(application, openTelemetryRum)

        verify {
            tracer.spanBuilder("AppStart")
        }
        verify {
            startupSpanBuilder.setAttribute(START_TYPE_KEY, "cold")
        }
        verify {
            startupSpanBuilder.startSpan()
        }
    }

    @Test
    fun `registers and unregisters activity lifecycle callbacks on install and uninstall`() {
        val mockApp = mockk<Application>(relaxed = true)
        val openTelemetryRum = createMockRum()
        val registeredCallbacks = mutableListOf<Application.ActivityLifecycleCallbacks>()

        activityLifecycleInstrumentation.install(mockApp, openTelemetryRum)

        verify(exactly = 2) {
            mockApp.registerActivityLifecycleCallbacks(capture(registeredCallbacks))
        }
        assertThat(registeredCallbacks).hasSize(2)

        activityLifecycleInstrumentation.uninstall(mockApp, openTelemetryRum)

        for (callback in registeredCallbacks) {
            verify(exactly = 1) {
                mockApp.unregisterActivityLifecycleCallbacks(callback)
            }
        }
    }

    @Test
    fun `repeated install is a safe no-op and does not register duplicate callbacks`() {
        val mockApp = mockk<Application>(relaxed = true)
        val openTelemetryRum = createMockRum()

        activityLifecycleInstrumentation.install(mockApp, openTelemetryRum)
        activityLifecycleInstrumentation.install(mockApp, openTelemetryRum)

        verify(exactly = 2) {
            mockApp.registerActivityLifecycleCallbacks(any())
        }
    }

    @Test
    fun `uninstall before install is a safe no-op`() {
        val mockApp = mockk<Application>(relaxed = true)
        val openTelemetryRum = createMockRum()

        activityLifecycleInstrumentation.uninstall(mockApp, openTelemetryRum)

        verify(exactly = 0) {
            mockApp.unregisterActivityLifecycleCallbacks(any())
        }
    }

    @Test
    fun `supports reinstall after uninstall`() {
        val mockApp = mockk<Application>(relaxed = true)
        val openTelemetryRum = createMockRum()

        activityLifecycleInstrumentation.install(mockApp, openTelemetryRum)
        activityLifecycleInstrumentation.uninstall(mockApp, openTelemetryRum)

        verify(exactly = 2) {
            mockApp.registerActivityLifecycleCallbacks(any())
            mockApp.unregisterActivityLifecycleCallbacks(any())
        }

        activityLifecycleInstrumentation.install(mockApp, openTelemetryRum)

        verify(exactly = 4) {
            mockApp.registerActivityLifecycleCallbacks(any())
        }
    }

    @Test
    fun `unwraps applicationContext when non-Application context is provided`() {
        val nonAppContext = mockk<android.content.Context>()
        val mockApp = mockk<Application>(relaxed = true)
        every { nonAppContext.applicationContext } returns mockApp

        val openTelemetryRum = createMockRum()

        activityLifecycleInstrumentation.install(nonAppContext, openTelemetryRum)

        verify(exactly = 2) {
            mockApp.registerActivityLifecycleCallbacks(any())
        }

        activityLifecycleInstrumentation.uninstall(nonAppContext, openTelemetryRum)

        verify(exactly = 2) {
            mockApp.unregisterActivityLifecycleCallbacks(any())
        }
    }

    @Test
    fun `non-Application context without Application in applicationContext is a safe no-op`() {
        val nonAppContext = mockk<android.content.Context>()
        every { nonAppContext.applicationContext } returns null

        val openTelemetryRum = createMockRum()

        activityLifecycleInstrumentation.install(nonAppContext, openTelemetryRum)
        activityLifecycleInstrumentation.uninstall(nonAppContext, openTelemetryRum)

        verify(exactly = 0) {
            openTelemetryRum.openTelemetry
        }
    }

    @Test
    fun `exposes correct name`() {
        assertThat(activityLifecycleInstrumentation.name).isEqualTo("activity")
    }

    private fun createOpenTelemetryRum(): OpenTelemetryRum {
        val openTelemetryRum = mockk<OpenTelemetryRum>()
        every { openTelemetryRum.openTelemetry } returns openTelemetry
        every { openTelemetryRum.sessionProvider } returns sessionProvider
        every { openTelemetryRum.clock } returns Clock.getDefault()
        return openTelemetryRum
    }

    private fun createMockRum(): OpenTelemetryRum {
        val rum = mockk<OpenTelemetryRum>(relaxed = true)
        val otel = mockk<io.opentelemetry.api.OpenTelemetry>(relaxed = true)
        every { rum.openTelemetry } returns otel
        every { rum.clock } returns Clock.getDefault()
        return rum
    }
}
