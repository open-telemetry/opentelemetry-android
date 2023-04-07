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
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.function.Function;
import java.util.function.Predicate;

/** Delegating wrapper around otel SpanFilterBuilder. */
public final class SpanFilterBuilder {

    private final io.opentelemetry.rum.internal.SpanFilterBuilder delegate =
            new io.opentelemetry.rum.internal.SpanFilterBuilder();
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
        delegate.rejectSpansByName(spanNamePredicate);
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
        delegate.rejectSpansByAttributeValue(attributeKey, attributeValuePredicate);
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
        delegate.removeSpanAttribute(attributeKey);
        return this;
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
        delegate.removeSpanAttribute(attributeKey, attributeValuePredicate);
        return this;
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
        delegate.replaceSpanAttribute(attributeKey, attributeValueModifier);
        return this;
    }

    io.opentelemetry.rum.internal.SpanFilterBuilder getDelegate() {
        return delegate;
    }

    public Function<SpanExporter, SpanExporter> build() {
        return delegate.build();
    }
}
