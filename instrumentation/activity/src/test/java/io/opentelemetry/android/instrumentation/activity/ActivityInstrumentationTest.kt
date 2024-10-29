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
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.internal.services.ServiceManager.Companion
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

@RunWith(AndroidJUnit4::class)
class ActivityInstrumentationTest {
    private lateinit var activityLifecycleInstrumentation: ActivityLifecycleInstrumentation
    private lateinit var application: Application
    private lateinit var openTelemetryRum: OpenTelemetryRum
    private lateinit var openTelemetry: OpenTelemetrySdk

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        openTelemetry = mockk()
        activityLifecycleInstrumentation = ActivityLifecycleInstrumentation()

        Companion.initialize(application)
    }

    @Test
    fun `Installing instrumentation starts AppStartupTimer`() {
        val tracer: Tracer = mockk()
        val startupSpanBuilder: SpanBuilder = mockk()
        val startupSpan: Span = mockk()

        every { openTelemetry.getTracer("io.opentelemetry.lifecycle") }.returns(tracer)
        every { tracer.spanBuilder("AppStart") }.returns(startupSpanBuilder)
        every { startupSpanBuilder.setStartTimestamp(any(), any()) }.returns(startupSpanBuilder)
        every { startupSpanBuilder.setAttribute(RumConstants.START_TYPE_KEY, "cold") }.returns(
            startupSpanBuilder,
        )
        every { startupSpanBuilder.startSpan() }.returns(startupSpan)

        val ctx = InstallationContext(application, openTelemetry, mockk())
        activityLifecycleInstrumentation.install(ctx)

        verify {
            tracer.spanBuilder("AppStart")
        }
        verify {
            startupSpanBuilder.setAttribute(RumConstants.START_TYPE_KEY, "cold")
        }
        verify {
            startupSpanBuilder.startSpan()
        }
    }
}
