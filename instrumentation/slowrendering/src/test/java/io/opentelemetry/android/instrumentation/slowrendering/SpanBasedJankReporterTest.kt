/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.sdk.trace.data.SpanData
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val COUNT_KEY = AttributeKey.longKey("count")

class SpanBasedJankReporterTest {
    private lateinit var tracer: Tracer

    @Rule
    var otelTesting: OpenTelemetryRule = OpenTelemetryRule.create()

    @BeforeEach
    fun setup() {
        tracer = otelTesting.openTelemetry.getTracer("testTracer")
    }

    @Test
    fun `spans are generated`() {
        val jankReporter = SpanBasedJankReporter(tracer)
        val histogramData = HashMap<Int, Int>()
        histogramData[17] = 3
        histogramData[701] = 1
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        jankReporter.reportSlow(histogramData, 0.1, "io.otel/Komponent")

        assertSpanContent(otelTesting.spans)
    }

    @Test
    fun `no spans created when no slow frames`() {
        val jankReporter = SpanBasedJankReporter(tracer)
        val histogramData = HashMap<Int, Int>()
        histogramData[3] = 3
        histogramData[8] = 1
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        jankReporter.reportSlow(histogramData, 0.1, "")

        assertThat(otelTesting.spans.size).isZero
    }

    private fun assertSpanContent(spans: MutableList<SpanData?>?) {
        assertThat<SpanData?>(spans)
            .hasSize(2)
            .satisfiesExactly(
                ThrowingConsumer { span: SpanData? ->
                    OpenTelemetryAssertions
                        .assertThat(span)
                        .hasName("slowRenders")
                        .endsAt(span!!.getStartEpochNanos())
                        .hasAttribute(COUNT_KEY, 3L)
                        .hasAttribute(
                            AttributeKey.stringKey("activity.name"),
                            "io.otel/Komponent",
                        )
                },
                ThrowingConsumer { span: SpanData? ->
                    OpenTelemetryAssertions
                        .assertThat(span)
                        .hasName("frozenRenders")
                        .endsAt(span!!.getStartEpochNanos())
                        .hasAttribute(COUNT_KEY, 1L)
                        .hasAttribute(
                            AttributeKey.stringKey("activity.name"),
                            "io.otel/Komponent",
                        )
                },
            )
    }
}
