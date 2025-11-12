/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.contrib.interceptor.InterceptableSpanExporter
import io.opentelemetry.contrib.interceptor.api.Interceptor
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.function.Predicate

@Suppress("UNCHECKED_CAST")
class FilteringSpanExporterBuilder internal constructor(
    private val delegate: SpanExporter,
) {
    private var predicate = Predicate { _: SpanData -> false }
    private val interceptor: Interceptor<SpanData> =
        Interceptor<SpanData> { item ->
            when {
                predicate.test(item) -> null
                else -> item
            }
        }

    /**
     * Creates a SpanExporter that will not export any spans whose name matches the given name. All
     * other spans will be exported by the delegate.
     *
     * @param name - Entire case sensitive span name to match for exclusion
     * @return a SpanExporter
     */
    fun rejectSpansNamed(name: String): FilteringSpanExporterBuilder = rejecting { span: SpanData -> name == span.name }

    /**
     * Creates a SpanExporter that will not export any spans whose name matches the given predicate.
     * All other spans will be exported by the delegate.
     *
     * @param spanNamePredicate - predicate to test the span name atainst
     * @return a SpanExporter
     */
    fun rejectSpansNamed(spanNamePredicate: Predicate<String>): FilteringSpanExporterBuilder =
        rejecting { span: SpanData -> spanNamePredicate.test(span.name) }

    /**
     * Creates a SpanExporter that will not export any spans whose name contains the given
     * substring. All other spans will be exported by the delegate.
     *
     * @param substring - Substring go match within the span name
     * @return a SpanExporter
     */
    fun rejectSpansWithNameContaining(substring: String): FilteringSpanExporterBuilder =
        rejecting { span: SpanData -> span.name.contains(substring) }

    /**
     * Creates a span exporter that will not export any spans whose SpanData matches the rejecting
     * predicate.
     *
     * @param predicate A predicate that returns true when a span is to be rejected
     * @return this
     */
    fun rejecting(predicate: Predicate<SpanData>): FilteringSpanExporterBuilder {
        this.predicate = this.predicate.or(predicate)
        return this
    }

    fun rejectSpansWithAttributesMatching(attrRejection: MutableMap<AttributeKey<*>, Predicate<*>>): FilteringSpanExporterBuilder {
        if (attrRejection.isEmpty()) {
            return this
        }
        val spanRejecter =
            Predicate { spanData: SpanData ->
                val attributes = spanData.attributes
                attrRejection.entries
                    .stream()
                    .anyMatch { e: MutableMap.MutableEntry<AttributeKey<*>, Predicate<*>> ->
                        val key: AttributeKey<*> = e.key
                        val valuePredicate =
                            e.value as Predicate<in Any?>
                        val attributeValue: Any? = attributes.get(key)
                        (
                            attributeValue != null &&
                                valuePredicate.test(attributeValue)
                        )
                    }
            }
        this.predicate = this.predicate.or(spanRejecter)
        return this
    }

    fun build(): SpanExporter = InterceptableSpanExporter(delegate, interceptor)
}
