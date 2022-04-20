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

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Arrays;
import org.junit.Test;

public class ModifiedSpanDataTest {
    private static final String TRACE_ID = TraceId.fromLongs(0, 42);
    private static final String SPAN_ID = SpanId.fromLong(123);

    @Test
    public void shouldForwardAllCallsExceptAttributesToTheOriginal() {
        SpanData original =
                TestSpanData.builder()
                        .setName("test")
                        .setKind(SpanKind.CLIENT)
                        .setSpanContext(
                                SpanContext.create(
                                        TRACE_ID,
                                        SPAN_ID,
                                        TraceFlags.getSampled(),
                                        TraceState.getDefault()))
                        .setParentSpanContext(SpanContext.getInvalid())
                        .setStatus(StatusData.ok())
                        .setStartEpochNanos(123)
                        .setAttributes(Attributes.of(stringKey("attribute"), "original value"))
                        .setEvents(emptyList())
                        .setLinks(emptyList())
                        .setEndEpochNanos(456)
                        .setHasEnded(true)
                        .setTotalRecordedEvents(0)
                        .setTotalRecordedLinks(0)
                        .setTotalAttributeCount(12)
                        .setInstrumentationLibraryInfo(
                                InstrumentationLibraryInfo.create("test", "0.0.1"))
                        .setResource(Resource.getDefault())
                        .build();

        SpanData modified =
                ModifiedSpanData.create(
                        original, Attributes.builder().put(stringKey("attribute"), "modified"));

        assertEquals(original.getName(), modified.getName());
        assertEquals(original.getKind(), modified.getKind());
        assertEquals(original.getSpanContext(), modified.getSpanContext());
        assertEquals(original.getParentSpanContext(), modified.getParentSpanContext());
        assertEquals(original.getStatus(), modified.getStatus());
        assertEquals(original.getStartEpochNanos(), modified.getStartEpochNanos());
        assertEquals(Attributes.of(stringKey("attribute"), "modified"), modified.getAttributes());
        assertEquals(original.getEvents(), modified.getEvents());
        assertEquals(original.getLinks(), modified.getLinks());
        assertEquals(original.getEndEpochNanos(), modified.getEndEpochNanos());
        assertEquals(original.hasEnded(), modified.hasEnded());
        assertEquals(original.getTotalRecordedEvents(), modified.getTotalRecordedEvents());
        assertEquals(original.getTotalRecordedLinks(), modified.getTotalRecordedLinks());
        assertEquals(1, modified.getTotalAttributeCount());
        assertEquals(
                original.getInstrumentationLibraryInfo(), modified.getInstrumentationLibraryInfo());
        assertEquals(original.getResource(), modified.getResource());
    }

    @Test
    public void shouldConvertExceptionEventsToSpanAttributes() {
        SpanData original =
                TestSpanData.builder()
                        .setName("test")
                        .setKind(SpanKind.CLIENT)
                        .setStatus(StatusData.unset())
                        .setStartEpochNanos(12345)
                        .setEndEpochNanos(67890)
                        .setHasEnded(true)
                        .setEvents(
                                Arrays.asList(
                                        EventData.create(
                                                123,
                                                "test",
                                                Attributes.of(stringKey("attribute"), "value")),
                                        EventData.create(
                                                456,
                                                SemanticAttributes.EXCEPTION_EVENT_NAME,
                                                Attributes.builder()
                                                        .put(
                                                                SemanticAttributes.EXCEPTION_TYPE,
                                                                "com.example.Error")
                                                        .put(
                                                                SemanticAttributes
                                                                        .EXCEPTION_MESSAGE,
                                                                "failed")
                                                        .put(
                                                                SemanticAttributes
                                                                        .EXCEPTION_STACKTRACE,
                                                                "<stacktrace>")
                                                        .build())))
                        .setAttributes(Attributes.of(stringKey("attribute"), "value"))
                        .build();

        SpanData modified = ModifiedSpanData.create(original);

        assertThat(modified)
                .hasName("test")
                .hasKind(SpanKind.CLIENT)
                .hasEvents(
                        EventData.create(
                                123, "test", Attributes.of(stringKey("attribute"), "value")))
                .hasTotalRecordedEvents(1)
                .hasAttributes(
                        Attributes.builder()
                                .put(stringKey("attribute"), "value")
                                .put(SemanticAttributes.EXCEPTION_TYPE, "Error")
                                .put(SplunkRum.ERROR_TYPE_KEY, "Error")
                                .put(SemanticAttributes.EXCEPTION_MESSAGE, "failed")
                                .put(SplunkRum.ERROR_MESSAGE_KEY, "failed")
                                .put(SemanticAttributes.EXCEPTION_STACKTRACE, "<stacktrace>")
                                .build())
                .hasTotalAttributeCount(6);
    }
}
