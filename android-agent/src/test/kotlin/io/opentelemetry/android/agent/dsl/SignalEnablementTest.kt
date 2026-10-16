/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SignalEnablementTest {
    private lateinit var otelConfig: OpenTelemetryConfiguration

    @BeforeEach
    fun setUp() {
        otelConfig =
            OpenTelemetryConfiguration(
                instrumentationLoader = FakeInstrumentationLoader(),
                clock = FakeClock(),
            )
    }

    @Test
    fun testDefaults() {
        assertThat(otelConfig.rumConfig.tracingEnabled).isTrue()
        assertThat(otelConfig.rumConfig.loggingEnabled).isTrue()
        assertThat(otelConfig.rumConfig.metricsEnabled).isTrue()
        assertThat(otelConfig.rumConfig.shouldGenerateSdkInitializationEvents()).isTrue()
    }

    @Test
    fun testDisableTracing() {
        assertThat(otelConfig.rumConfig.tracingEnabled).isTrue()
        otelConfig.disableTracing()
        assertThat(otelConfig.rumConfig.tracingEnabled).isFalse()
    }

    @Test
    fun testDisableLogging() {
        assertThat(otelConfig.rumConfig.loggingEnabled).isTrue()
        otelConfig.disableLogging()
        assertThat(otelConfig.rumConfig.loggingEnabled).isFalse()
    }

    @Test
    fun testDisableMetrics() {
        assertThat(otelConfig.rumConfig.metricsEnabled).isTrue()
        otelConfig.disableMetrics()
        assertThat(otelConfig.rumConfig.metricsEnabled).isFalse()
    }

    @Test
    fun testDisableSdkInitializationEvents() {
        assertThat(otelConfig.rumConfig.shouldGenerateSdkInitializationEvents()).isTrue()
        otelConfig.disableSdkInitializationEvents()
        assertThat(otelConfig.rumConfig.shouldGenerateSdkInitializationEvents()).isFalse()
    }
}
