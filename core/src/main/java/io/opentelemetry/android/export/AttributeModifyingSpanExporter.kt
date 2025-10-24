/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

typealias SpanAttributeReplacement = (AttributeKey<*>, Any?) -> Any?

/**
 * A SpanExporter that is configured to modify some of its attributes at export time.
 */
class AttributeModifyingSpanExporter(
    private val delegate: SpanExporter,
    private val spanAttributeReplacements: SpanAttributeReplacement,
) : SpanExporter {
    override fun export(spans: Collection<SpanData>): CompletableResultCode = delegate.export(spans.map(::buildModifiedAttributes))

    private fun buildModifiedAttributes(span: SpanData): ModifiedSpanData {
        val originals = span.attributes.asMap()
        val modified =
            originals.mapValues { entry ->
                spanAttributeReplacements(entry.key, entry.value)
            }

        val builder = Attributes.builder()
        modified.forEach {
            @Suppress("UNCHECKED_CAST")
            builder.put(it.key as AttributeKey<Any>, it.value)
        }
        return ModifiedSpanData(span, builder.build())
    }

    override fun flush(): CompletableResultCode = delegate.flush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()
}
