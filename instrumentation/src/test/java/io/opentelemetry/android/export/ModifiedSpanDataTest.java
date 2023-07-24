/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.android.export.ModifiedSpanData;
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
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import org.junit.jupiter.api.Test;

class ModifiedSpanDataTest {
    private static final String TRACE_ID = TraceId.fromLongs(0, 42);
    private static final String SPAN_ID = SpanId.fromLong(123);

    @Test
    void shouldForwardAllCallsExceptAttributesToTheOriginal() {
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
                new ModifiedSpanData(original, Attributes.of(stringKey("attribute"), "modified"));

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
}
