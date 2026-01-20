/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.function.Function
import java.util.function.Predicate

/**
 * A utility that can be used to create a SpanExporter that allows filtering and modification of
 * span data before it is sent to Allows modification of span data before it is sent to a delegate
 * exporter. Spans can be rejected entirely based on their name or attribute content, or their
 * attributes may be modified.
 */
class SpanDataModifier private constructor(
    private val delegate: SpanExporter
) {

    private var rejectSpanNamesPredicate: Predicate<String> = Predicate { false }
    private val rejectSpanAttributesPredicates = mutableMapOf<AttributeKey<*>, Predicate<*>>()

    private val spanAttributeReplacements = mutableMapOf<AttributeKey<*>, Function<*, *>>()


    /**
     * Remove matching spans from the exporter pipeline.
     *
     * <p>Spans with names that match the {@code spanNamePredicate} will not be exported.
     *
     * @param spanNamePredicate A function that returns true if a span with passed name should be
     *     rejected.
     * @return {@code this}.
     */
    fun rejectSpansByName(spanNamePredicate: Predicate<String>): SpanDataModifier {
        rejectSpanNamesPredicate = rejectSpanNamesPredicate.or(spanNamePredicate)
        return this
    }

    /**
     * Remove matching spans from the exporter pipeline.
     *
     * <p>Any span that contains an attribute with key {@code attributeKey} and value matching the
     * {@code attributeValuePredicate} will not be exported.
     *
     * @param attributeKey An attribute key to match.
     * @param attributeValuePredicate A function that returns true if a span containing an attribute
     *     with matching value should be rejected.
     * @return {@code this}.
     */
    fun <T> rejectSpansByAttributeValue(
        attributeKey: AttributeKey<T>,
        attributeValuePredicate: Predicate<in T>
    ): SpanDataModifier {
        rejectSpanAttributesPredicates.compute(attributeKey) { _, oldValue ->
            if (oldValue == null) {
                attributeValuePredicate
            } else {
                @Suppress("UNCHECKED_CAST")
                (oldValue as Predicate<T>).or(attributeValuePredicate)
            }
        }
        return this
    }

    /**
     * Modify span data before it enters the exporter pipeline.
     *
     * <p>Any attribute with key {@code attributeKey} and will be removed from the span before it is
     * exported.
     *
     * @param attributeKey An attribute key to match.
     * @return {@code this}.
     */
    fun <T> removeSpanAttribute(attributeKey: AttributeKey<T>): SpanDataModifier {
        return removeSpanAttribute(attributeKey, Predicate { true })
    }

    /**
     * Modify span data before it enters the exporter pipeline.
     *
     * <p>Any attribute with key {@code attributeKey} and value matching the {@code
     * attributeValuePredicate} will be removed from the span before it is exported.
     *
     * @param attributeKey An attribute key to match.
     * @param attributeValuePredicate A function that returns true if an attribute with matching
     *     value should be removed from the span.
     * @return {@code this}.
     */


    fun <T> removeSpanAttribute(
        attributeKey: AttributeKey<T>,
        attributeValuePredicate: Predicate<in T>
    ): SpanDataModifier {
        return replaceSpanAttribute(attributeKey, Function { old ->
            if (attributeValuePredicate.test(old)) null else old
        })
    }

    /**
     * Modify span data before it enters the exporter pipeline.
     *
     * <p>The value of any attribute with key {@code attributeKey} will be passed to the {@code
     * attributeValueModifier} function. The value returned by the function will replace the
     * original value. When the modifier function returns {@code null} the attribute will be removed
     * from the span.
     *
     * @param attributeKey An attribute key to match.
     * @param attributeValueModifier A function that receives the old attribute value and returns
     *     the new one.
     * @return {@code this}.
     */
    fun <T> replaceSpanAttribute(
        attributeKey: AttributeKey<T>,
        attributeValueModifier: Function<in T, out T?>
    ): SpanDataModifier {
        spanAttributeReplacements.compute(attributeKey) { _, oldValue ->
            if (oldValue == null) {
                attributeValueModifier
            } else {
                @Suppress("UNCHECKED_CAST")
                val previous = oldValue as Function<T, T?>
                Function<T, T?> { t: T ->
                    val intermediate = previous.apply(t)
                    if (intermediate == null) null else attributeValueModifier.apply(intermediate)
                }
            }
        }
        return this
    }


    fun build(): SpanExporter {
        var modifier = delegate

        if (spanAttributeReplacements.isNotEmpty()) {
            modifier = AttributeModifyingSpanExporter(
                delegate
            ) { attributeKey, value ->
                @Suppress("UNCHECKED_CAST")
                val function = spanAttributeReplacements[attributeKey] as? Function<Any?, Any?>
                if (function != null) {
                    function.apply(value)
                } else {
                    value
                }
            }
        }

        return FilteringSpanExporter.builder(modifier)
            .rejectSpansWithAttributesMatching(HashMap(rejectSpanAttributesPredicates))
            .rejectSpansNamed(rejectSpanNamesPredicate)
            .build()
    }

    companion object {
        /**
         * Creates a new SpanDataModifier builder.
         *
         * @param delegate The underlying SpanExporter to wrap.
         * @return A new SpanDataModifier instance.
         */
        @JvmStatic
        fun builder(delegate: SpanExporter): SpanDataModifier {
            return SpanDataModifier(delegate)
        }
    }
}
