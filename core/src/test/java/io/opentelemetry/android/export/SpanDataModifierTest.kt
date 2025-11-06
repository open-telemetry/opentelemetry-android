/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.export.TestSpanHelper.span
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.function.Function
import java.util.function.Predicate

@ExtendWith(MockKExtension::class)
internal class SpanDataModifierTest {
    @RelaxedMockK
    private lateinit var delegate: SpanExporter
    private lateinit var spansCaptor: CapturingSlot<MutableCollection<SpanData>>

    @BeforeEach
    fun init() {
        spansCaptor = slot<MutableCollection<SpanData>>()
    }

    @Test
    fun shouldRejectSpansByName() {
        // given
        val underTest =
            SpanDataModifier
                .builder(delegate)
                .rejectSpansByName { it == "span2" }
                .rejectSpansByName { it == "span4" }
                .build()

        val span1 = span("span1")
        val span2 = span("span2")
        val span3 = span("span3")
        val span4 = span("span4")

        val expectedResult = CompletableResultCode()
        every { delegate.export(capture(spansCaptor)) } returns expectedResult

        // when
        val result = underTest.export(listOf(span1, span2, span3, span4))

        // then
        assertSame(expectedResult, result)

        assertThat(spansCaptor.captured)
            .satisfiesExactly(
                ThrowingConsumer {
                    OpenTelemetryAssertions.assertThat(it).hasName(span1.name)
                },
                ThrowingConsumer {
                    OpenTelemetryAssertions.assertThat(it).hasName(span3.name)
                },
            )
    }

    @Test
    fun shouldRejectSpansByAttributeValue() {
        // given
        val underTest =
            SpanDataModifier
                .builder(delegate)
                .rejectSpansByAttributeValue(
                    ATTRIBUTE,
                    Predicate { it == "test" },
                ).rejectSpansByAttributeValue(
                    ATTRIBUTE,
                    Predicate { it == "rejected!" },
                ).rejectSpansByAttributeValue(
                    LONG_ATTRIBUTE,
                    Predicate { value: Long -> value > 100 },
                ).build()

        val rejected = span("span", Attributes.of(ATTRIBUTE, "test"))
        val differentKey =
            span(
                "span",
                Attributes.of(OTHER_ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L),
            )
        val anotherRejected =
            span("span", Attributes.of(ATTRIBUTE, "rejected!"))
        val differentValue =
            span("span", Attributes.of(ATTRIBUTE, "not really test"))
        val yetAnotherRejected =
            span("span", Attributes.of(ATTRIBUTE, "pass", LONG_ATTRIBUTE, 123L))

        val expectedResult = CompletableResultCode()
        every { delegate.export(capture(spansCaptor)) } returns expectedResult

        // when
        val result =
            underTest.export(
                listOf(
                    rejected,
                    differentKey,
                    anotherRejected,
                    differentValue,
                    yetAnotherRejected,
                ),
            )

        // then
        assertSame(expectedResult, result)

        assertThat(spansCaptor.captured)
            .satisfiesExactly(
                ThrowingConsumer {
                    OpenTelemetryAssertions
                        .assertThat(it)
                        .hasName(differentKey.name)
                        .hasAttributes(differentKey.attributes)
                },
                ThrowingConsumer {
                    OpenTelemetryAssertions
                        .assertThat(it)
                        .hasName(differentValue.name)
                        .hasAttributes(differentValue.attributes)
                },
            )
    }

    @Test
    fun shouldRemoveSpanAttributes() {
        // given
        val underTest =
            SpanDataModifier
                .builder(delegate)
                .removeSpanAttribute(
                    ATTRIBUTE,
                    Predicate { it == "test" },
                ) // make sure that attribute types are taken into account
                .removeSpanAttribute(AttributeKey.stringKey("long_attribute"))
                .build()

        val span1 =
            span("first", Attributes.of(ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L))
        val span2 =
            span(
                "second",
                Attributes.of(ATTRIBUTE, "not test", OTHER_ATTRIBUTE, "test"),
            )

        val expectedResult = CompletableResultCode()
        every { delegate.export(capture(spansCaptor)) } returns expectedResult

        // when
        val result = underTest.export(listOf(span1, span2))

        // then
        assertSame(expectedResult, result)

        val exportedSpans = spansCaptor.captured.toList()
        assertEquals(2, exportedSpans.size)
        assertEquals("first", exportedSpans[0].name)
        assertEquals(
            Attributes.of(LONG_ATTRIBUTE, 42L),
            exportedSpans[0].attributes,
        )
        assertEquals("second", exportedSpans[1].name)
        assertEquals(
            Attributes.of(ATTRIBUTE, "not test", OTHER_ATTRIBUTE, "test"),
            exportedSpans[1].attributes,
        )
    }

    @Test
    fun shouldReplaceSpanAttributes() {
        // given
        val underTest =
            SpanDataModifier
                .builder(delegate)
                .replaceSpanAttribute(
                    ATTRIBUTE,
                    Function { "$it!!!" },
                ).replaceSpanAttribute(
                    ATTRIBUTE,
                    Function { it + "1" },
                ).replaceSpanAttribute(
                    LONG_ATTRIBUTE,
                    Function { value: Long -> value + 1 },
                ) // make sure that attribute types are taken into account
                .replaceSpanAttribute(
                    AttributeKey.stringKey("long_attribute"),
                    Function { "abc" },
                ).build()

        val span1 =
            span("first", Attributes.of(ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L))
        val span2 = span("second", Attributes.of(OTHER_ATTRIBUTE, "test"))

        val expectedResult = CompletableResultCode()
        every { delegate.export(capture(spansCaptor)) } returns expectedResult

        // when
        val result = underTest.export(listOf(span1, span2))

        // then
        assertSame(expectedResult, result)

        val exportedSpans = spansCaptor.captured.toList()
        assertEquals(2, exportedSpans.size)
        assertEquals("first", exportedSpans[0].name)
        assertEquals(
            Attributes.of(ATTRIBUTE, "test!!!1", LONG_ATTRIBUTE, 43L),
            exportedSpans[0].attributes,
        )
        assertEquals("second", exportedSpans[1].name)
        assertEquals(
            Attributes.of(OTHER_ATTRIBUTE, "test"),
            exportedSpans[1].attributes,
        )
    }

    @Test
    fun shouldReplaceSpanAttributes_removeAttributeByReturningNull() {
        // given
        val underTest =
            SpanDataModifier
                .builder(delegate)
                .replaceSpanAttribute(ATTRIBUTE, Function { null })
                .build()

        val span =
            span("first", Attributes.of(ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L))

        val expectedResult = CompletableResultCode()
        every { delegate.export(capture(spansCaptor)) } returns expectedResult

        // when
        val result = underTest.export(listOf(span))

        // then
        assertSame(expectedResult, result)

        val exportedSpans = spansCaptor.captured.toList()
        assertEquals(1, exportedSpans.size)
        assertEquals("first", exportedSpans[0].name)
        assertEquals(
            Attributes.of(LONG_ATTRIBUTE, 42L),
            exportedSpans[0].attributes,
        )
    }

    @Test
    fun builderChangesShouldNotApplyToAlreadyDecoratedExporter() {
        // given
        val builder = SpanDataModifier.builder(delegate)
        val underTest = builder.build()

        builder
            .rejectSpansByName { it == "span" }
            .rejectSpansByAttributeValue(ATTRIBUTE, Predicate { true })
            .removeSpanAttribute(ATTRIBUTE, Predicate { true })
            .replaceSpanAttribute(ATTRIBUTE, Function { "abc" })

        val span = span("span", Attributes.of(ATTRIBUTE, "test"))

        val expectedResult = CompletableResultCode()
        every { delegate.export(capture(spansCaptor)) } returns expectedResult

        // when
        val result = underTest.export(listOf(span))

        // then
        assertSame(expectedResult, result)

        assertThat(spansCaptor.captured)
            .satisfiesExactly(
                ThrowingConsumer {
                    OpenTelemetryAssertions
                        .assertThat(it)
                        .hasName(span.name)
                        .hasAttributes(span.attributes)
                },
            )
    }

    @Test
    fun shouldDelegateCalls() {
        val underTest = SpanDataModifier.builder(delegate).build()

        underTest.flush()
        verify(exactly = 1) { delegate.flush() }

        underTest.shutdown()
        verify(exactly = 1) { delegate.shutdown() }
    }

    private companion object {
        val ATTRIBUTE: AttributeKey<String?> = AttributeKey.stringKey("attribute")
        val OTHER_ATTRIBUTE: AttributeKey<String?> = AttributeKey.stringKey("other_attribute")
        val LONG_ATTRIBUTE: AttributeKey<Long?> = AttributeKey.longKey("long_attribute")
    }
}
