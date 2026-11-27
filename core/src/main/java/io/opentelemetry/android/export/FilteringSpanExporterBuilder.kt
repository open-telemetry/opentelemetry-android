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
    private var predicate: (SpanData) -> Boolean = { false }
    private val interceptor: Interceptor<SpanData> =
        Interceptor<SpanData> { item ->
            when {
                predicate(item) -> null
                else -> item
            }
        }

    /**
     * Creates a SpanExporter that will not export any spans whose name matches the given name. All
     * other spans will be exported by the delegate.
     *
     * @param name - Entire case-sensitive span name to match for exclusion
     * @return a SpanExporter
     */
    fun rejectSpansNamed(name: String): FilteringSpanExporterBuilder = rejecting { span: SpanData -> name == span.name }

    /**
     * Creates a SpanExporter that will not export any spans whose name matches the given predicate.
     * All other spans will be exported by the delegate.
     *
     * @param spanNamePredicate - predicate to test the span name against
     * @return a SpanExporter
     */
    fun rejectSpansNamed(spanNamePredicate: (String) -> Boolean): FilteringSpanExporterBuilder =
        rejecting { span: SpanData -> spanNamePredicate(span.name) }

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
    fun rejecting(predicate: (SpanData) -> Boolean): FilteringSpanExporterBuilder {
        val previous = this.predicate
        this.predicate = { span -> previous(span) || predicate(span) }
        return this
    }

    fun rejectSpansWithMatchingAttributes(
        attrRejection: MutableMap<AttributeKey<*>, Function1<*, Boolean>>,
    ): FilteringSpanExporterBuilder = rejectMatchingAttributes(attrRejection) { match, value -> (match as (Any) -> Boolean)(value) }

    fun build(): SpanExporter = InterceptableSpanExporter(delegate, interceptor)

    // Keep these descriptors for callers compiled against the original builder.
    @Deprecated("Retained for binary compatibility until 1.11.0.", level = DeprecationLevel.HIDDEN)
    fun rejectSpansNamed(spanNamePredicate: Predicate<String>): FilteringSpanExporterBuilder = rejectSpansNamed(spanNamePredicate::test)

    @Deprecated("Retained for binary compatibility until 1.11.0.", level = DeprecationLevel.HIDDEN)
    fun rejecting(predicate: Predicate<SpanData>): FilteringSpanExporterBuilder = rejecting(predicate::test)

    @Deprecated("Retained for binary compatibility until 1.11.0.", level = DeprecationLevel.HIDDEN)
    fun rejectSpansWithAttributesMatching(attrRejection: MutableMap<AttributeKey<*>, Predicate<*>>): FilteringSpanExporterBuilder =
        rejectLegacyAttributes(attrRejection)

    internal fun rejectLegacyAttributes(attrRejection: MutableMap<AttributeKey<*>, Predicate<*>>): FilteringSpanExporterBuilder =
        rejectMatchingAttributes(attrRejection) { match, value -> (match as Predicate<Any>).test(value) }

    private fun <P> rejectMatchingAttributes(
        attrRejection: MutableMap<AttributeKey<*>, P>,
        matches: (P, Any) -> Boolean,
    ): FilteringSpanExporterBuilder {
        if (attrRejection.isEmpty()) {
            return this
        }
        return rejecting { span ->
            val attributes = span.attributes
            attrRejection.any { (key, valuePredicate) ->
                val attributeValue = attributes.get(key)
                attributeValue != null && matches(valuePredicate, attributeValue)
            }
        }
    }
}
