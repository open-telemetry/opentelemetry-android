/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignalEnablementTest {
    private lateinit var otelConfig: OpenTelemetryConfiguration

    @Before
    fun setUp() {
        otelConfig =
            OpenTelemetryConfiguration(
                instrumentationLoader = FakeInstrumentationLoader(),
                clock = FakeClock(),
            )
    }

    @Test
    fun testDefaults() {
        assertTrue(otelConfig.rumConfig.tracingEnabled)
        assertTrue(otelConfig.rumConfig.loggingEnabled)
        assertTrue(otelConfig.rumConfig.metricsEnabled)
    }

    @Test
    fun testToggleTracingEnablement() {
        otelConfig.disableTracing(true)
        assertFalse(otelConfig.rumConfig.tracingEnabled)

        otelConfig.disableTracing(false)
        assertTrue(otelConfig.rumConfig.tracingEnabled)
    }

    @Test
    fun testToggleLoggingEnablement() {
        otelConfig.disableLogging(true)
        assertFalse(otelConfig.rumConfig.loggingEnabled)

        otelConfig.disableLogging(false)
        assertTrue(otelConfig.rumConfig.loggingEnabled)
    }

    @Test
    fun testToggleMetricsEnablement() {
        otelConfig.disableMetrics(true)
        assertFalse(otelConfig.rumConfig.metricsEnabled)

        otelConfig.disableMetrics(false)
        assertTrue(otelConfig.rumConfig.metricsEnabled)
    }
}
