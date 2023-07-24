/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal.export;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.rum.internal.export.TestSpanHelper.span;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttributeModifyingSpanExporterTest {

    @Mock SpanExporter exporter;
    @Captor ArgumentCaptor<Collection<SpanData>> spansCaptor;

    @Test
    void testEmptyMap() {
        SpanData span1 = span("span1");
        SpanData span2 = span("span2");
        SpanData span3 = span("span3");
        Collection<SpanData> spans = Arrays.asList(span1, span2, span3);
        CompletableResultCode expectedResult = mock(CompletableResultCode.class);
        when(exporter.export(spans)).thenReturn(expectedResult);

        AttributeModifyingSpanExporter underTest =
                new AttributeModifyingSpanExporter(exporter, emptyMap());

        CompletableResultCode result = underTest.export(spans);
        assertSame(expectedResult, result);
    }

    @Test
    void testRemappedToNull() {
        AttributeKey<String> key = stringKey("foo");
        SpanData span1 = span("span1", Attributes.of(key, "bar"));
        Collection<SpanData> originalSpans = Collections.singletonList(span1);

        Map<AttributeKey<?>, Function<?, ?>> remappers = new HashMap<>();
        remappers.put(key, s -> null);

        CompletableResultCode expectedResult = mock(CompletableResultCode.class);
        when(exporter.export(spansCaptor.capture())).thenReturn(expectedResult);

        AttributeModifyingSpanExporter underTest =
                new AttributeModifyingSpanExporter(exporter, remappers);

        underTest.export(originalSpans);
        assertThat(spansCaptor.getValue())
                .satisfiesExactly(span -> assertThat(span).hasTotalAttributeCount(0));
    }

    @Test
    void modify() {
        Attributes attr1 = buildAttr(1);
        SpanData span1 = span("span1", attr1);
        Attributes attr2 = buildAttr(2);
        SpanData span2 = span("span2", attr2);
        Attributes attr3 = buildAttr(3);
        SpanData span3 = span("span3", attr3);
        Collection<SpanData> spans = Arrays.asList(span1, span2, span3);
        Map<AttributeKey<?>, Function<?, ?>> modifiers = new HashMap<>();
        modifiers.put(stringKey("foo1"), x -> "" + x + x);
        modifiers.put(stringKey("foo3"), x -> "3" + x + x);
        modifiers.put(stringKey("boop2"), x -> "2" + x + x);

        CompletableResultCode expectedResult = mock(CompletableResultCode.class);
        when(exporter.export(spansCaptor.capture())).thenReturn(expectedResult);

        AttributeModifyingSpanExporter underTest =
                new AttributeModifyingSpanExporter(exporter, modifiers);
        CompletableResultCode result = underTest.export(spans);
        assertSame(expectedResult, result);
        assertThat(spansCaptor.getValue())
                .satisfiesExactly(
                        s ->
                                assertThat(s)
                                        .hasAttributesSatisfyingExactly(
                                                equalTo(stringKey("foo1"), "bar1bar1"),
                                                equalTo(stringKey("bar1"), "baz1"),
                                                equalTo(stringKey("boop1"), "beep1")),
                        s ->
                                assertThat(s)
                                        .hasAttributesSatisfyingExactly(
                                                equalTo(stringKey("foo2"), "bar2"),
                                                equalTo(stringKey("bar2"), "baz2"),
                                                equalTo(stringKey("boop2"), "2beep2beep2")),
                        s ->
                                assertThat(s)
                                        .hasAttributesSatisfyingExactly(
                                                equalTo(stringKey("foo3"), "3bar3bar3"),
                                                equalTo(stringKey("bar3"), "baz3"),
                                                equalTo(stringKey("boop3"), "beep3")));
    }

    private static Attributes buildAttr(int num) {
        return Attributes.builder()
                .put("foo" + num, "bar" + num)
                .put("bar" + num, "baz" + num)
                .put("boop" + num, "beep" + num)
                .build();
    }
}
