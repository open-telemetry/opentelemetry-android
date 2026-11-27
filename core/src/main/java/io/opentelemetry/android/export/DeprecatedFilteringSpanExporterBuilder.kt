/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.function.Predicate

private const val DEPRECATION_MESSAGE = "Use FilteringSpanExporter.with. This builder will be removed in 1.11.0."

@Deprecated(DEPRECATION_MESSAGE)
class DeprecatedFilteringSpanExporterBuilder internal constructor(
    delegate: SpanExporter,
) {
    private val builder = FilteringSpanExporterBuilder(delegate)

    /**
     * Creates a SpanExporter that will not export any spans whose name matches the given name. All
     * other spans will be exported by the delegate.
     *
     * @param name - Entire case-sensitive span name to match for exclusion
     * @return a SpanExporter
     */
    @Deprecated(DEPRECATION_MESSAGE)
    fun rejectSpansNamed(name: String): DeprecatedFilteringSpanExporterBuilder = apply { builder.rejectSpansNamed(name) }

    /**
     * Creates a SpanExporter that will not export any spans whose name matches the given predicate.
     * All other spans will be exported by the delegate.
     *
     * @param spanNamePredicate - predicate to test the span name against
     * @return a SpanExporter
     */
    @Deprecated(DEPRECATION_MESSAGE)
    fun rejectSpansNamed(spanNamePredicate: Predicate<String>): DeprecatedFilteringSpanExporterBuilder =
        apply { builder.rejectSpansNamed(spanNamePredicate::test) }

    /**
     * Creates a SpanExporter that will not export any spans whose name contains the given
     * substring. All other spans will be exported by the delegate.
     *
     * @param substring - Substring go match within the span name
     * @return a SpanExporter
     */
    @Deprecated(DEPRECATION_MESSAGE)
    fun rejectSpansWithNameContaining(substring: String): DeprecatedFilteringSpanExporterBuilder =
        apply { builder.rejectSpansWithNameContaining(substring) }

    /**
     * Creates a span exporter that will not export any spans whose SpanData matches the rejecting
     * predicate.
     *
     * @param predicate A predicate that returns true when a span is to be rejected
     * @return this
     */
    @Deprecated(DEPRECATION_MESSAGE)
    fun rejecting(predicate: Predicate<SpanData>): DeprecatedFilteringSpanExporterBuilder = apply { builder.rejecting(predicate::test) }

    @Deprecated(DEPRECATION_MESSAGE)
    fun rejectSpansWithAttributesMatching(
        attrRejection: MutableMap<AttributeKey<*>, Predicate<*>>,
    ): DeprecatedFilteringSpanExporterBuilder = apply { builder.rejectLegacyAttributes(attrRejection) }

    @Deprecated(DEPRECATION_MESSAGE)
    fun build(): SpanExporter = builder.build()
}
