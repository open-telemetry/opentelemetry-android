/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export;

import static io.opentelemetry.android.export.TestSpanHelper.span;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpanDataModifierTest {
    static final AttributeKey<String> ATTRIBUTE = stringKey("attribute");
    static final AttributeKey<String> OTHER_ATTRIBUTE = stringKey("other_attribute");
    static final AttributeKey<Long> LONG_ATTRIBUTE = longKey("long_attribute");

    @Mock SpanExporter delegate;

    @Captor ArgumentCaptor<Collection<SpanData>> spansCaptor;

    @Test
    void shouldRejectSpansByName() {
        // given
        SpanExporter underTest =
                new SpanDataModifier()
                        .rejectSpansByName(spanName -> spanName.equals("span2"))
                        .rejectSpansByName(spanName -> spanName.equals("span4"))
                        .wrap(delegate);

        SpanData span1 = TestSpanHelper.span("span1");
        SpanData span2 = TestSpanHelper.span("span2");
        SpanData span3 = TestSpanHelper.span("span3");
        SpanData span4 = TestSpanHelper.span("span4");

        CompletableResultCode expectedResult = new CompletableResultCode();
        when(delegate.export(spansCaptor.capture())).thenReturn(expectedResult);

        // when
        CompletableResultCode result = underTest.export(asList(span1, span2, span3, span4));

        // then
        assertSame(expectedResult, result);

        assertThat(spansCaptor.getValue())
                .satisfiesExactly(
                        s -> assertThat(s).hasName(span1.getName()),
                        s -> assertThat(s).hasName(span3.getName()));
    }

    @Test
    void shouldRejectSpansByAttributeValue() {
        // given
        SpanExporter underTest =
                new SpanDataModifier()
                        .rejectSpansByAttributeValue(ATTRIBUTE, value -> value.equals("test"))
                        .rejectSpansByAttributeValue(ATTRIBUTE, value -> value.equals("rejected!"))
                        .rejectSpansByAttributeValue(LONG_ATTRIBUTE, value -> value > 100)
                        .wrap(delegate);

        SpanData rejected = TestSpanHelper.span("span", Attributes.of(ATTRIBUTE, "test"));
        SpanData differentKey =
                TestSpanHelper.span(
                        "span", Attributes.of(OTHER_ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L));
        SpanData anotherRejected =
                TestSpanHelper.span("span", Attributes.of(ATTRIBUTE, "rejected!"));
        SpanData differentValue =
                TestSpanHelper.span("span", Attributes.of(ATTRIBUTE, "not really test"));
        SpanData yetAnotherRejected =
                TestSpanHelper.span("span", Attributes.of(ATTRIBUTE, "pass", LONG_ATTRIBUTE, 123L));

        CompletableResultCode expectedResult = new CompletableResultCode();
        when(delegate.export(spansCaptor.capture())).thenReturn(expectedResult);

        // when
        CompletableResultCode result =
                underTest.export(
                        asList(
                                rejected,
                                differentKey,
                                anotherRejected,
                                differentValue,
                                yetAnotherRejected));

        // then
        assertSame(expectedResult, result);

        assertThat(spansCaptor.getValue())
                .satisfiesExactly(
                        s ->
                                assertThat(s)
                                        .hasName(differentKey.getName())
                                        .hasAttributes(differentKey.getAttributes()),
                        s ->
                                assertThat(s)
                                        .hasName(differentValue.getName())
                                        .hasAttributes(differentValue.getAttributes()));
    }

    @Test
    void shouldRemoveSpanAttributes() {
        // given
        SpanExporter underTest =
                new SpanDataModifier()
                        .removeSpanAttribute(ATTRIBUTE, value -> value.equals("test"))
                        // make sure that attribute types are taken into account
                        .removeSpanAttribute(stringKey("long_attribute"))
                        .wrap(delegate);

        SpanData span1 =
                TestSpanHelper.span("first", Attributes.of(ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L));
        SpanData span2 =
                TestSpanHelper.span(
                        "second", Attributes.of(ATTRIBUTE, "not test", OTHER_ATTRIBUTE, "test"));

        CompletableResultCode expectedResult = new CompletableResultCode();
        when(delegate.export(spansCaptor.capture())).thenReturn(expectedResult);

        // when
        CompletableResultCode result = underTest.export(asList(span1, span2));

        // then
        assertSame(expectedResult, result);

        List<SpanData> exportedSpans = new ArrayList<>(spansCaptor.getValue());
        assertEquals(2, exportedSpans.size());
        assertEquals("first", exportedSpans.get(0).getName());
        assertEquals(Attributes.of(LONG_ATTRIBUTE, 42L), exportedSpans.get(0).getAttributes());
        assertEquals("second", exportedSpans.get(1).getName());
        assertEquals(
                Attributes.of(ATTRIBUTE, "not test", OTHER_ATTRIBUTE, "test"),
                exportedSpans.get(1).getAttributes());
    }

    @Test
    void shouldReplaceSpanAttributes() {
        // given
        SpanExporter underTest =
                new SpanDataModifier()
                        .replaceSpanAttribute(ATTRIBUTE, value -> value + "!!!")
                        .replaceSpanAttribute(ATTRIBUTE, value -> value + "1")
                        .replaceSpanAttribute(LONG_ATTRIBUTE, value -> value + 1)
                        // make sure that attribute types are taken into account
                        .replaceSpanAttribute(stringKey("long_attribute"), value -> "abc")
                        .wrap(delegate);

        SpanData span1 =
                TestSpanHelper.span("first", Attributes.of(ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L));
        SpanData span2 = TestSpanHelper.span("second", Attributes.of(OTHER_ATTRIBUTE, "test"));

        CompletableResultCode expectedResult = new CompletableResultCode();
        when(delegate.export(spansCaptor.capture())).thenReturn(expectedResult);

        // when
        CompletableResultCode result = underTest.export(asList(span1, span2));

        // then
        assertSame(expectedResult, result);

        List<SpanData> exportedSpans = new ArrayList<>(spansCaptor.getValue());
        assertEquals(2, exportedSpans.size());
        assertEquals("first", exportedSpans.get(0).getName());
        assertEquals(
                Attributes.of(ATTRIBUTE, "test!!!1", LONG_ATTRIBUTE, 43L),
                exportedSpans.get(0).getAttributes());
        assertEquals("second", exportedSpans.get(1).getName());
        assertEquals(Attributes.of(OTHER_ATTRIBUTE, "test"), exportedSpans.get(1).getAttributes());
    }

    @Test
    void shouldReplaceSpanAttributes_removeAttributeByReturningNull() {
        // given
        SpanExporter underTest =
                new SpanDataModifier()
                        .replaceSpanAttribute(ATTRIBUTE, value -> null)
                        .wrap(delegate);

        SpanData span =
                TestSpanHelper.span("first", Attributes.of(ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L));

        CompletableResultCode expectedResult = new CompletableResultCode();
        when(delegate.export(spansCaptor.capture())).thenReturn(expectedResult);

        // when
        CompletableResultCode result = underTest.export(singletonList(span));

        // then
        assertSame(expectedResult, result);

        List<SpanData> exportedSpans = new ArrayList<>(spansCaptor.getValue());
        assertEquals(1, exportedSpans.size());
        assertEquals("first", exportedSpans.get(0).getName());
        assertEquals(Attributes.of(LONG_ATTRIBUTE, 42L), exportedSpans.get(0).getAttributes());
    }

    @Test
    void builderChangesShouldNotApplyToAlreadyDecoratedExporter() {
        // given
        SpanDataModifier builder = new SpanDataModifier();
        SpanExporter underTest = builder.wrap(delegate);

        builder.rejectSpansByName(spanName -> spanName.equals("span"))
                .rejectSpansByAttributeValue(ATTRIBUTE, value -> true)
                .removeSpanAttribute(ATTRIBUTE, value -> true)
                .replaceSpanAttribute(ATTRIBUTE, value -> "abc");

        SpanData span = TestSpanHelper.span("span", Attributes.of(ATTRIBUTE, "test"));

        CompletableResultCode expectedResult = new CompletableResultCode();
        when(delegate.export(spansCaptor.capture())).thenReturn(expectedResult);

        // when
        CompletableResultCode result = underTest.export(singletonList(span));

        // then
        assertSame(expectedResult, result);

        assertThat(spansCaptor.getValue())
                .satisfiesExactly(
                        s ->
                                assertThat(s)
                                        .hasName(span.getName())
                                        .hasAttributes(span.getAttributes()));
    }

    @Test
    void shouldDelegateCalls() {
        SpanExporter underTest = new SpanDataModifier().wrap(delegate);

        underTest.flush();
        verify(delegate).flush();

        underTest.shutdown();
        verify(delegate).shutdown();
    }
}
