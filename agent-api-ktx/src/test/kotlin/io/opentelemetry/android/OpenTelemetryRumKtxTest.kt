/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalApi::class)

package io.opentelemetry.android

import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpenTelemetryRumKtxTest {
    private lateinit var spanExporter: InMemorySpanExporter
    private lateinit var rum: OpenTelemetryRum

    @BeforeEach
    fun setUp() {
        spanExporter = InMemorySpanExporter.create()
        val sdk =
            OpenTelemetrySdk
                .builder()
                .setTracerProvider(
                    SdkTracerProvider
                        .builder()
                        .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                        .build(),
                ).build()
        rum = mockk<OpenTelemetryRum>()
        every { rum.openTelemetry } returns sdk
    }

    @Test
    fun `records telemetry through the underlying java sdk`() {
        rum.openTelemetryKotlin
            .tracerProvider
            .getTracer("test-scope")
            .startSpan("test-span")
            .end()

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.name).isEqualTo("test-span")
        assertThat(span.instrumentationScopeInfo.name).isEqualTo("test-scope")
    }

    @Test
    fun `returns the same instance for repeated access`() {
        assertThat(rum.openTelemetryKotlin).isSameAs(rum.openTelemetryKotlin)
    }
}
