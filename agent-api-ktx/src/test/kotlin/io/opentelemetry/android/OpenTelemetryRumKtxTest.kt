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
import io.opentelemetry.sdk.common.Clock
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
        every { rum.clock } returns FIXED_CLOCK
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
    fun `uses the clock from the rum instance`() {
        rum.openTelemetryKotlin
            .tracerProvider
            .getTracer("test-scope")
            .startSpan("test-span")
            .end()
        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.startEpochNanos).isEqualTo(FIXED_TIME_NANOS)
    }

    @Test
    fun `returns the same instance for repeated access`() {
        assertThat(rum.openTelemetryKotlin).isSameAs(rum.openTelemetryKotlin)
    }

    private companion object {
        const val FIXED_TIME_NANOS = 1_234_567_890_000L

        val FIXED_CLOCK =
            object : Clock {
                override fun now(): Long = FIXED_TIME_NANOS

                override fun nanoTime(): Long = FIXED_TIME_NANOS
            }
    }
}
