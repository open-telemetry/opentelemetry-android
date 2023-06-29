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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.rum.internal.export.SpanDataModifier;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/** Delegating wrapper around otel SpanDataModifier. */
public final class SpanFilterBuilder {

    // TODO: Use pure upstream mechanism for this
    private Predicate<String> rejectSpanNamesPredicate = spanName -> false;
    private final Map<AttributeKey<?>, Predicate<?>> rejectSpanAttributesPredicates =
            new HashMap<>();
    private final Map<AttributeKey<?>, Function<?, ?>> spanAttributeReplacements = new HashMap<>();

    /**
     * Remove matching spans from the exporter pipeline.
     *
     * <p>Spans with names that match the {@code spanNamePredicate} will not be exported.
     *
     * @param spanNamePredicate A function that returns true if a span with passed name should be
     *     rejected.
     * @return {@code this}.
     */
    public SpanFilterBuilder rejectSpansByName(Predicate<String> spanNamePredicate) {
        this.rejectSpanNamesPredicate = rejectSpanNamesPredicate.or(spanNamePredicate);
        return this;
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
    public <T> SpanFilterBuilder rejectSpansByAttributeValue(
            AttributeKey<T> attributeKey, Predicate<? super T> attributeValuePredicate) {
        rejectSpanAttributesPredicates.compute(
                attributeKey,
                (k, oldValue) ->
                        oldValue == null
                                ? attributeValuePredicate
                                : ((Predicate<T>) oldValue).or(attributeValuePredicate));
        return this;
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
    public <T> SpanFilterBuilder removeSpanAttribute(AttributeKey<T> attributeKey) {
        return removeSpanAttribute(attributeKey, value -> true);
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
    public <T> SpanFilterBuilder removeSpanAttribute(
            AttributeKey<T> attributeKey, Predicate<? super T> attributeValuePredicate) {

        return replaceSpanAttribute(
                attributeKey, old -> attributeValuePredicate.test(old) ? null : old);
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
    public <T> SpanFilterBuilder replaceSpanAttribute(
            AttributeKey<T> attributeKey, Function<? super T, ? extends T> attributeValueModifier) {
        spanAttributeReplacements.compute(
                attributeKey,
                (k, oldValue) ->
                        oldValue == null
                                ? attributeValueModifier
                                : ((Function<T, T>) oldValue).andThen(attributeValueModifier));
        return this;
    }

    SpanExporter build(SpanExporter exporter) {
        SpanDataModifier builder =
                SpanDataModifier.builder(exporter).rejectSpansByName(rejectSpanNamesPredicate);
        spanAttributeReplacements.forEach(
                (attributeKey, function) -> {
                    AttributeKey<? super Object> key = (AttributeKey<? super Object>) attributeKey;
                    Function<? super Object, ? super Object> fn =
                            (Function<? super Object, ? super Object>) function;
                    builder.replaceSpanAttribute(key, fn);
                });
        rejectSpanAttributesPredicates.forEach(
                ((attributeKey, predicate) -> {
                    AttributeKey<? super Object> kk = (AttributeKey<? super Object>) attributeKey;
                    Predicate<? super Object> vv = (Predicate<? super Object>) predicate;
                    builder.rejectSpansByAttributeValue(kk, vv);
                }));
        return builder.build();
    }
}
