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
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AttributeModifyingSpanExporterTest {
    private lateinit var exporter: InMemorySpanExporter

    @BeforeEach
    fun setUp() {
        exporter = InMemorySpanExporter.create()
    }

    @Test
    fun testEmptyMap() {
        val span1 = span("span1")
        val span2 = span("span2")
        val span3 = span("span3")
        val spans = listOf(span1, span2, span3)
        val underTest = AttributeModifyingSpanExporter(exporter) { _, value -> value }

        val result = underTest.export(spans)
        assertSame(CompletableResultCode.ofSuccess(), result)
    }

    @Test
    fun testRemappedToNull() {
        val key = AttributeKey.stringKey("foo")
        val span1 = span("span1", Attributes.of(key, "bar"))
        val originalSpans = listOf(span1)
        val underTest = AttributeModifyingSpanExporter(exporter) { _, value -> null }
        val result = underTest.export(originalSpans)

        assertSame(CompletableResultCode.ofSuccess(), result)
        assertThat(exporter.finishedSpanItems)
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

        val underTest =
            AttributeModifyingSpanExporter(exporter) { key, value ->
                when (key) {
                    AttributeKey.stringKey("foo1") -> "$value$value"
                    AttributeKey.stringKey("foo3") -> "3$value$value"
                    AttributeKey.stringKey("boop2") -> "2$value$value"
                    else -> value
                }
            }
        val result = underTest.export(spans)
        assertSame(CompletableResultCode.ofSuccess(), result)
        assertAttributes()
    }

    private fun assertAttributes() {
        assertThat(exporter.finishedSpanItems)
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
