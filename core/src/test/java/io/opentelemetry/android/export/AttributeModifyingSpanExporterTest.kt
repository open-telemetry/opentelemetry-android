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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.function.Function

@ExtendWith(MockitoExtension::class)
internal class AttributeModifyingSpanExporterTest {
    @Mock
    private lateinit var exporter: SpanExporter

    @Captor
    private lateinit var spansCaptor: ArgumentCaptor<MutableCollection<SpanData>>

    @Test
    fun testEmptyMap() {
        val span1 = span("span1")
        val span2 = span("span2")
        val span3 = span("span3")
        val spans = listOf(span1, span2, span3)
        val expectedResult: CompletableResultCode? = mock(CompletableResultCode::class.java)
        `when`(exporter.export(spans)).thenReturn(expectedResult)

        val underTest =
            AttributeModifyingSpanExporter(exporter, emptyMap())

        val result = underTest.export(spans)
        assertSame(expectedResult, result)
    }

    @Test
    fun testRemappedToNull() {
        val key = AttributeKey.stringKey("foo")
        val span1 = span("span1", Attributes.of(key, "bar"))
        val originalSpans = listOf(span1)

        val remappers = mutableMapOf<AttributeKey<*>, Function<*, *>>()
        remappers.put(key, Function { _: Any? -> null })

        val expectedResult = Mockito.mock(CompletableResultCode::class.java)
        `when`(exporter.export(spansCaptor.capture()))
            .thenReturn(expectedResult)

        val underTest =
            AttributeModifyingSpanExporter(exporter, remappers)

        underTest.export(originalSpans)
        assertThat(spansCaptor.getValue())
            .satisfiesExactly(
                ThrowingConsumer { span: SpanData? ->
                    OpenTelemetryAssertions
                        .assertThat(
                            span,
                        ).hasTotalAttributeCount(0)
                },
            )
    }

    @Test
    fun modify() {
        val attr1 = buildAttr(1)
        val span1 = span("span1", attr1)
        val attr2 = buildAttr(2)
        val span2 = span("span2", attr2)
        val attr3 = buildAttr(3)
        val span3 = span("span3", attr3)
        val spans = listOf(span1, span2, span3)
        val modifiers = mutableMapOf<AttributeKey<*>, Function<*, *>>()
        modifiers.put(AttributeKey.stringKey("foo1"), Function { x: Any? -> "" + x + x })
        modifiers.put(AttributeKey.stringKey("foo3"), Function { x: Any? -> "3$x$x" })
        modifiers.put(AttributeKey.stringKey("boop2"), Function { x: Any? -> "2$x$x" })

        val expectedResult = Mockito.mock(CompletableResultCode::class.java)
        `when`(exporter.export(spansCaptor.capture()))
            .thenReturn(expectedResult)

        val underTest = AttributeModifyingSpanExporter(exporter, modifiers)
        val result = underTest.export(spans)
        assertSame(expectedResult, result)
        assertAttributes()
    }

    private fun assertAttributes() {
        assertThat(spansCaptor.getValue())
            .satisfiesExactly(
                ThrowingConsumer {
                    OpenTelemetryAssertions
                        .assertThat(it)
                        .hasAttributesSatisfyingExactly(
                            OpenTelemetryAssertions.equalTo(
                                AttributeKey.stringKey("foo1"),
                                "bar1bar1",
                            ),
                            OpenTelemetryAssertions.equalTo(
                                AttributeKey.stringKey("bar1"),
                                "baz1",
                            ),
                            OpenTelemetryAssertions.equalTo(
                                AttributeKey.stringKey("boop1"),
                                "beep1",
                            ),
                        )
                },
                ThrowingConsumer {
                    OpenTelemetryAssertions
                        .assertThat(it)
                        .hasAttributesSatisfyingExactly(
                            OpenTelemetryAssertions.equalTo(
                                AttributeKey.stringKey("foo2"),
                                "bar2",
                            ),
                            OpenTelemetryAssertions.equalTo(
                                AttributeKey.stringKey("bar2"),
                                "baz2",
                            ),
                            OpenTelemetryAssertions.equalTo(
                                AttributeKey.stringKey("boop2"),
                                "2beep2beep2",
                            ),
                        )
                },
                ThrowingConsumer {
                    OpenTelemetryAssertions
                        .assertThat(it)
                        .hasAttributesSatisfyingExactly(
                            OpenTelemetryAssertions.equalTo(
                                AttributeKey.stringKey("foo3"),
                                "3bar3bar3",
                            ),
                            OpenTelemetryAssertions.equalTo(
                                AttributeKey.stringKey("bar3"),
                                "baz3",
                            ),
                            OpenTelemetryAssertions.equalTo(
                                AttributeKey.stringKey("boop3"),
                                "beep3",
                            ),
                        )
                },
            )
    }

    private fun buildAttr(num: Int): Attributes =
        Attributes
            .builder()
            .put("foo$num", "bar$num")
            .put("bar$num", "baz$num")
            .put("boop$num", "beep$num")
            .build()
}
