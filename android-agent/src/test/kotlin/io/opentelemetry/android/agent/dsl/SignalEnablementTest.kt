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
    fun testDisableTracing() {
        assertTrue(otelConfig.rumConfig.tracingEnabled)
        otelConfig.disableTracing()
        assertFalse(otelConfig.rumConfig.tracingEnabled)

    }

    @Test
    fun testDisableLogging() {
        assertTrue(otelConfig.rumConfig.loggingEnabled)
        otelConfig.disableLogging()
        assertFalse(otelConfig.rumConfig.loggingEnabled)
    }

    @Test
    fun testDisableMetrics() {
        assertTrue(otelConfig.rumConfig.metricsEnabled)
        otelConfig.disableMetrics()
        assertFalse(otelConfig.rumConfig.metricsEnabled)
    }
}
