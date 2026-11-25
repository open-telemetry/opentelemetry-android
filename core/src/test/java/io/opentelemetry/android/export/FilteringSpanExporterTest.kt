/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.opentelemetry.android.export.TestSpanHelper.span
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.Test

internal class FilteringSpanExporterTest {
    @Test
    fun filter() {
        val span1 = span("one")
        val span2 = span("two")
        val span3 = span("three")
        val span4 = span("four")
        val span5 = span("FIVE")
        val attr6 = Attributes.of(AttributeKey.stringKey("herp"), "derp")
        val span6 = span("six", attr6)
        val attr7 = Attributes.of(AttributeKey.stringKey("dig"), "dug")
        val span7 = span("seven", attr7)
        val span8 = span("eight")
        val spans = listOf(span1, span2, span3, span4, span5, span6, span7, span8)

        val attrRejects =
            mutableMapOf<AttributeKey<*>, Function1<*, Boolean>>(
                AttributeKey.stringKey("herp") to { value: String -> value == "derp" },
                AttributeKey.stringKey("dig") to { value: String -> value.startsWith("d") },
            )

        val exporter = mockk<SpanExporter>()
        val expectedResult = mockk<CompletableResultCode>()

        val spansCaptor = slot<MutableCollection<SpanData>>()
        every { exporter.export(capture(spansCaptor)) } returns expectedResult

        val underTest =
            FilteringSpanExporter.with(exporter) {
                rejecting { it === span2 }
                rejectSpansWithNameContaining("hree")
                rejectSpansNamed("four")
                rejectSpansNamed { it.equals("five", ignoreCase = true) }
                rejectSpansWithMatchingAttributes(attrRejects)
            }

        val result = underTest.export(spans)
        assertThat(result).isSameAs(expectedResult)
        val resultSpans = spansCaptor.captured
        assertThat(resultSpans)
            .satisfiesExactly(
                ThrowingConsumer { s: SpanData? ->
                    OpenTelemetryAssertions.assertThat(s).isSameAs(span1)
                },
                ThrowingConsumer { s: SpanData? ->
                    OpenTelemetryAssertions.assertThat(s).isSameAs(span8)
                },
            )
    }
}
