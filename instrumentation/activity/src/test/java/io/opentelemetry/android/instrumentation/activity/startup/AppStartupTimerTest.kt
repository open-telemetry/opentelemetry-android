/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.startup

import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.api.trace.Tracer
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
        private var otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private lateinit var tracer: Tracer

    @BeforeEach
    fun setup() {
        tracer = otelTesting.openTelemetry.getTracer("testTracer")
    }

    @Test
    fun start_end() {
        val appStartupTimer = AppStartupTimer()
        val startSpan = appStartupTimer.start(tracer)
        assertNotNull(startSpan)
        appStartupTimer.end()

        val spans = otelTesting.spans
        assertEquals(1, spans.size)
        val spanData = spans[0]

        assertEquals("AppStart", spanData.name)
        assertEquals(
            "cold",
            spanData.attributes.get<String>(RumConstants.START_TYPE_KEY),
        )
    }

    @Test
    fun multi_end() {
        val appStartupTimer = AppStartupTimer()
        appStartupTimer.start(tracer)
        appStartupTimer.end()
        appStartupTimer.end()

        assertEquals(1, otelTesting.spans.size)
    }

    @Test
    fun multi_start() {
        val appStartupTimer = AppStartupTimer()
        appStartupTimer.start(tracer)
        assertSame(appStartupTimer.start(tracer), appStartupTimer.start(tracer))

        appStartupTimer.end()
        assertEquals(1, otelTesting.spans.size)
    }
}
