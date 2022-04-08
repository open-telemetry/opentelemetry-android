/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

@RunWith(MockitoJUnitRunner.class)
public class SpanDataModifierTest {
    static final AttributeKey<String> ATTRIBUTE = stringKey("attribute");
    static final AttributeKey<String> OTHER_ATTRIBUTE = stringKey("other_attribute");
    static final AttributeKey<Long> LONG_ATTRIBUTE = longKey("long_attribute");

    @Mock
    SpanExporter delegate;

    @Captor
    ArgumentCaptor<Collection<SpanData>> spansCaptor;

    @Test
    public void shouldRejectSpansByName() {
        // given
        SpanExporter underTest = new SpanFilterBuilder()
                .rejectSpansByName(spanName -> spanName.equals("span2"))
                .rejectSpansByName(spanName -> spanName.equals("span4"))
                .build()
                .apply(delegate);

        SpanData span1 = span("span1");
        SpanData span2 = span("span2");
        SpanData span3 = span("span3");
        SpanData span4 = span("span4");

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
    public void shouldRejectSpansByAttributeValue() {
        // given
        SpanExporter underTest = new SpanFilterBuilder()
                .rejectSpansByAttributeValue(ATTRIBUTE, value -> value.equals("test"))
                .rejectSpansByAttributeValue(ATTRIBUTE, value -> value.equals("rejected!"))
                .rejectSpansByAttributeValue(LONG_ATTRIBUTE, value -> value > 100)
                .build()
                .apply(delegate);

        SpanData rejected = span("span", Attributes.of(ATTRIBUTE, "test"));
        SpanData differentKey = span("span", Attributes.of(OTHER_ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L));
        SpanData anotherRejected = span("span", Attributes.of(ATTRIBUTE, "rejected!"));
        SpanData differentValue = span("span", Attributes.of(ATTRIBUTE, "not really test"));
        SpanData yetAnotherRejected = span("span", Attributes.of(ATTRIBUTE, "pass", LONG_ATTRIBUTE, 123L));

        CompletableResultCode expectedResult = new CompletableResultCode();
        when(delegate.export(spansCaptor.capture())).thenReturn(expectedResult);

        // when
        CompletableResultCode result = underTest.export(
                asList(rejected, differentKey, anotherRejected, differentValue, yetAnotherRejected));

        // then
        assertSame(expectedResult, result);

        assertThat(spansCaptor.getValue())
                .satisfiesExactly(
                        s -> assertThat(s)
                                .hasName(differentKey.getName())
                                .hasAttributes(differentKey.getAttributes()),
                        s -> assertThat(s)
                                .hasName(differentValue.getName())
                                .hasAttributes(differentValue.getAttributes()));
    }

    @Test
    public void shouldRemoveSpanAttributes() {
        // given
        SpanExporter underTest = new SpanFilterBuilder()
                .removeSpanAttribute(ATTRIBUTE, value -> value.equals("test"))
                // make sure that attribute types are taken into account
                .removeSpanAttribute(stringKey("long_attribute"))
                .build()
                .apply(delegate);

        SpanData span1 = span("first", Attributes.of(ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L));
        SpanData span2 = span("second", Attributes.of(ATTRIBUTE, "not test", OTHER_ATTRIBUTE, "test"));

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
        assertEquals(Attributes.of(ATTRIBUTE, "not test", OTHER_ATTRIBUTE, "test"), exportedSpans.get(1).getAttributes());
    }

    @Test
    public void shouldReplaceSpanAttributes() {
        // given
        SpanExporter underTest = new SpanFilterBuilder()
                .replaceSpanAttribute(ATTRIBUTE, value -> value + "!!!")
                .replaceSpanAttribute(ATTRIBUTE, value -> value + "1")
                .replaceSpanAttribute(LONG_ATTRIBUTE, value -> value + 1)
                // make sure that attribute types are taken into account
                .replaceSpanAttribute(stringKey("long_attribute"), value -> "abc")
                .build()
                .apply(delegate);

        SpanData span1 = span("first", Attributes.of(ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L));
        SpanData span2 = span("second", Attributes.of(OTHER_ATTRIBUTE, "test"));

        CompletableResultCode expectedResult = new CompletableResultCode();
        when(delegate.export(spansCaptor.capture())).thenReturn(expectedResult);

        // when
        CompletableResultCode result = underTest.export(asList(span1, span2));

        // then
        assertSame(expectedResult, result);

        List<SpanData> exportedSpans = new ArrayList<>(spansCaptor.getValue());
        assertEquals(2, exportedSpans.size());
        assertEquals("first", exportedSpans.get(0).getName());
        assertEquals(Attributes.of(ATTRIBUTE, "test!!!1", LONG_ATTRIBUTE, 43L), exportedSpans.get(0).getAttributes());
        assertEquals("second", exportedSpans.get(1).getName());
        assertEquals(Attributes.of(OTHER_ATTRIBUTE, "test"), exportedSpans.get(1).getAttributes());
    }

    @Test
    public void shouldReplaceSpanAttributes_removeAttributeByReturningNull() {
        // given
        SpanExporter underTest = new SpanFilterBuilder()
                .replaceSpanAttribute(ATTRIBUTE, value -> null)
                .build()
                .apply(delegate);

        SpanData span = span("first", Attributes.of(ATTRIBUTE, "test", LONG_ATTRIBUTE, 42L));

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
    public void builderChangesShouldNotApplyToAlreadyDecoratedExporter() {
        // given
        SpanFilterBuilder builder = new SpanFilterBuilder();
        SpanExporter underTest = builder
                .build()
                .apply(delegate);

        builder.rejectSpansByName(spanName -> spanName.equals("span"))
                .rejectSpansByAttributeValue(ATTRIBUTE, value -> true)
                .removeSpanAttribute(ATTRIBUTE, value -> true)
                .replaceSpanAttribute(ATTRIBUTE, value -> "abc");

        SpanData span = span("span", Attributes.of(ATTRIBUTE, "test"));

        CompletableResultCode expectedResult = new CompletableResultCode();
        when(delegate.export(spansCaptor.capture())).thenReturn(expectedResult);

        // when
        CompletableResultCode result = underTest.export(singletonList(span));

        // then
        assertSame(expectedResult, result);

        assertThat(spansCaptor.getValue())
                .satisfiesExactly(
                        s -> assertThat(s)
                                .hasName(span.getName())
                                .hasAttributes(span.getAttributes()));
    }

    @Test
    public void shouldDelegateCalls() {
        SpanExporter underTest = new SpanFilterBuilder()
                .build()
                .apply(delegate);

        underTest.flush();
        verify(delegate).flush();

        underTest.shutdown();
        verify(delegate).shutdown();
    }

    @Test
    public void shouldAlwaysTransformExceptionEvents() {
        // given
        SpanExporter underTest = new SpanFilterBuilder().build().apply(delegate);

        String stacktrace = "com.example.SomeException: boom!\n\tat com.example.SomeClass.method(SomeClass.java:123)";
        EventData exceptionEvent = EventData.create(123, SemanticAttributes.EXCEPTION_EVENT_NAME,
                Attributes.builder()
                        .put(SemanticAttributes.EXCEPTION_TYPE, "com.example.SomeException")
                        .put(SemanticAttributes.EXCEPTION_MESSAGE, "boom!")
                        .put(SemanticAttributes.EXCEPTION_STACKTRACE, stacktrace)
                        .build());
        SpanData span = span("span", Attributes.of(ATTRIBUTE, "test"), exceptionEvent);

        CompletableResultCode expectedResult = new CompletableResultCode();
        when(delegate.export(spansCaptor.capture())).thenReturn(expectedResult);

        // when
        CompletableResultCode result = underTest.export(singletonList(span));

        // then
        assertSame(expectedResult, result);

        assertThat(spansCaptor.getValue())
                .satisfiesExactly(
                        s -> assertThat(s)
                                .hasName(span.getName())
                                .hasEvents(Collections.emptyList())
                                .hasTotalRecordedEvents(0)
                                .hasAttributes(span.getAttributes().toBuilder()
                                        .put(SemanticAttributes.EXCEPTION_TYPE, "SomeException")
                                        .put(SplunkRum.ERROR_TYPE_KEY, "SomeException")
                                        .put(SemanticAttributes.EXCEPTION_MESSAGE, "boom!")
                                        .put(SplunkRum.ERROR_MESSAGE_KEY, "boom!")
                                        .put(SemanticAttributes.EXCEPTION_STACKTRACE, stacktrace)
                                        .build())
                                .hasTotalAttributeCount(6));
    }

    private static SpanData span(String name) {
        return span(name, Attributes.empty());
    }

    private static SpanData span(String name, Attributes attributes, EventData... events) {
        return TestSpanData.builder()
                .setName(name)
                .setKind(SpanKind.INTERNAL)
                .setStatus(StatusData.unset())
                .setHasEnded(true)
                .setStartEpochNanos(0)
                .setEndEpochNanos(123)
                .setAttributes(attributes)
                .setEvents(Arrays.asList(events))
                .build();
    }
}