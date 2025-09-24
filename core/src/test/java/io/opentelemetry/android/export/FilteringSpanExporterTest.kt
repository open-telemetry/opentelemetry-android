/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

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
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.function.Predicate

@ExtendWith(MockitoExtension::class)
internal class FilteringSpanExporterTest {
    @Captor
    private lateinit var spansCaptor: ArgumentCaptor<MutableCollection<SpanData>>

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

        val attrRejects = mutableMapOf<AttributeKey<*>, Predicate<*>>()
        attrRejects.put(
            AttributeKey.stringKey("herp"),
            Predicate { anObject: String -> "derp".equals(anObject) },
        )
        attrRejects.put(
            AttributeKey.stringKey("dig"),
            Predicate { v: String -> v.startsWith("d") },
        )

        val exporter = Mockito.mock(SpanExporter::class.java)
        val expectedResult = Mockito.mock(CompletableResultCode::class.java)

        Mockito
            .`when`(exporter.export(spansCaptor.capture()))
            .thenReturn(expectedResult)

        val underTest =
            FilteringSpanExporter
                .builder(exporter)
                .rejecting { x: SpanData -> x === span2 }
                .rejectSpansWithNameContaining("hree")
                .rejectSpansNamed("four")
                .rejectSpansNamed { x: String -> x.equals("five", ignoreCase = true) }
                .rejectSpansWithAttributesMatching(attrRejects)
                .build()

        val result = underTest.export(spans)
        assertThat(result).isSameAs(expectedResult)
        val resultSpans = spansCaptor.getValue()
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
