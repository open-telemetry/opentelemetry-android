/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.api.logs.LoggerProvider
import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DisableableOpenTelemetryTest {
    private val delegate: OpenTelemetrySdk = OpenTelemetrySdk.builder().build()

    @Test
    fun `delegates every provider when all signals enabled`() {
        val result =
            DisableableOpenTelemetry(
                delegate = delegate,
                tracingEnabled = true,
                loggingEnabled = true,
                metricsEnabled = true,
            )

        assertThat(result.tracerProvider).isSameAs(delegate.tracerProvider)
        assertThat(result.logsBridge).isSameAs(delegate.logsBridge)
        assertThat(result.meterProvider).isSameAs(delegate.meterProvider)
        assertThat(result.propagators).isSameAs(delegate.propagators)
    }

    @Test
    fun `provide no-ops for any disabled api`() {
        val result =
            DisableableOpenTelemetry(
                delegate = delegate,
                tracingEnabled = false,
                loggingEnabled = false,
                metricsEnabled = false,
            )

        assertThat(result.tracerProvider).isSameAs(TracerProvider.noop())
        assertThat(result.logsBridge).isSameAs(LoggerProvider.noop())
        assertThat(result.meterProvider).isSameAs(MeterProvider.noop())
        assertThat(result.propagators).isSameAs(delegate.propagators)
    }
}
