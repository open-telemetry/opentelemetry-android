/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanId
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceId
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.common.InstrumentationScopeInfo
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.testing.trace.TestSpanData
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ModifiedSpanDataTest {
    private val original: SpanData =
        TestSpanData
            .builder()
            .setName("test")
            .setKind(SpanKind.CLIENT)
            .setSpanContext(
                SpanContext.create(
                    TRACE_ID,
                    SPAN_ID,
                    TraceFlags.getSampled(),
                    TraceState.getDefault(),
                ),
            ).setParentSpanContext(SpanContext.getInvalid())
            .setStatus(StatusData.ok())
            .setStartEpochNanos(123)
            .setAttributes(
                Attributes.of(
                    AttributeKey.stringKey("attribute"),
                    "original value",
                ),
            ).setEvents(mutableListOf<EventData?>())
            .setLinks(mutableListOf<LinkData?>())
            .setEndEpochNanos(456)
            .setHasEnded(true)
            .setTotalRecordedEvents(0)
            .setTotalRecordedLinks(0)
            .setTotalAttributeCount(12)
            .setInstrumentationScopeInfo(
                InstrumentationScopeInfo
                    .builder("test")
                    .setVersion("0.0.1")
                    .build(),
            ).setResource(Resource.getDefault())
            .build()

    @Test
    fun shouldForwardAllCallsExceptAttributesToTheOriginal() {
        val modified: SpanData =
            ModifiedSpanData(
                original,
                Attributes.of(AttributeKey.stringKey("attribute"), "modified"),
            )

        assertEquals(original.name, modified.name)
        assertEquals(original.kind, modified.kind)
        assertEquals(original.spanContext, modified.spanContext)
        assertEquals(original.parentSpanContext, modified.parentSpanContext)
        assertEquals(original.status, modified.status)
        assertEquals(original.startEpochNanos, modified.startEpochNanos)
        assertEquals(
            Attributes.of(
                AttributeKey.stringKey("attribute"),
                "modified",
            ),
            modified.attributes,
        )
        assertEquals(original.events, modified.events)
        assertEquals(original.links, modified.links)
        assertEquals(original.endEpochNanos, modified.endEpochNanos)
        assertEquals(original.hasEnded(), modified.hasEnded())
        assertEquals(
            original.totalRecordedEvents,
            modified.totalRecordedEvents,
        )
        assertEquals(original.totalRecordedLinks, modified.totalRecordedLinks)
        assertEquals(1, modified.totalAttributeCount)
        assertEquals(
            original.instrumentationScopeInfo,
            modified.instrumentationScopeInfo,
        )
        assertEquals(original.resource, modified.resource)
    }

    private companion object {
        private val TRACE_ID: String = TraceId.fromLongs(0, 42)
        private val SPAN_ID: String = SpanId.fromLong(123)
    }
}
