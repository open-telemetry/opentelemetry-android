/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.startup

import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class AppStartupTimerTest {
    companion object {
        @RegisterExtension
        private val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private lateinit var tracer: Tracer

    @BeforeEach
    fun setup() {
        tracer = otelTesting.openTelemetry.getTracer("testTracer")
    }

    @Test
    fun start_end() {
        val appStartupTimer = AppStartupTimer()
        val startSpan = appStartupTimer.start(tracer, Clock.getDefault())
        assertNotNull(startSpan)
        appStartupTimer.end()

        val spans = otelTesting.spans
        assertEquals(1, spans.size)
        val spanData = spans[0]

        assertEquals("AppStart", spanData.name)
        assertEquals(
            "cold",
            spanData.attributes.get(RumConstants.START_TYPE_KEY),
        )
    }

    @Test
    fun multi_end() {
        val appStartupTimer = AppStartupTimer()
        appStartupTimer.start(tracer, Clock.getDefault())
        appStartupTimer.end()
        appStartupTimer.end()

        assertEquals(1, otelTesting.spans.size)
    }

    @Test
    fun multi_start() {
        val appStartupTimer = AppStartupTimer()
        val clock = Clock.getDefault()
        appStartupTimer.start(tracer, clock)
        assertSame(appStartupTimer.start(tracer, clock), appStartupTimer.start(tracer, clock))

        appStartupTimer.end()
        assertEquals(1, otelTesting.spans.size)
    }
}
