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

package io.opentelemetry.rum.internal.export;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Map;
import java.util.function.Predicate;

public final class FilteringSpanExporterBuilder {

    private final SpanExporter delegate;
    private Predicate<SpanData> predicate = x -> false;

    FilteringSpanExporterBuilder(SpanExporter spanExporter) {
        this.delegate = spanExporter;
    }

    /**
     * Creates a SpanExporter that will not export any spans whose name matches the given name. All
     * other spans will be exported by the delegate.
     *
     * @param name - Entire case sensitive span name to match for exclusion
     * @return a SpanExporter
     */
    public FilteringSpanExporterBuilder rejectSpansNamed(String name) {
        return rejecting(span -> name.equals(span.getName()));
    }

    /**
     * Creates a SpanExporter that will not export any spans whose name matches the given predicate.
     * All other spans will be exported by the delegate.
     *
     * @param spanNamePredicate - predicate to test the span name atainst
     * @return a SpanExporter
     */
    public FilteringSpanExporterBuilder rejectSpansNamed(Predicate<String> spanNamePredicate) {
        return rejecting(span -> spanNamePredicate.test(span.getName()));
    }

    /**
     * Creates a SpanExporter that will not export any spans whose name contains the given
     * substring. All other spans will be exported by the delegate.
     *
     * @param substring - Substring go match within the span name
     * @return a SpanExporter
     */
    public FilteringSpanExporterBuilder rejectSpansWithNameContaining(String substring) {
        return rejecting(span -> span.getName().contains(substring));
    }

    /**
     * Creates a span exporter that will not export any spans whose SpanData matches the rejecting
     * predicate.
     *
     * @param predicate A predicate that returns true when a span is to be rejected
     * @return this
     */
    public FilteringSpanExporterBuilder rejecting(Predicate<SpanData> predicate) {
        this.predicate = this.predicate.or(predicate);
        return this;
    }

    public FilteringSpanExporterBuilder rejectSpansWithAttributesMatching(
            Map<AttributeKey<?>, Predicate<?>> attrRejection) {
        if (attrRejection.isEmpty()) {
            return this;
        }
        Predicate<SpanData> spanRejecter =
                spanData -> {
                    Attributes attributes = spanData.getAttributes();
                    return attrRejection.entrySet().stream()
                            .anyMatch(
                                    e -> {
                                        AttributeKey<?> key = e.getKey();
                                        Predicate<? super Object> valuePredicate =
                                                (Predicate<? super Object>) e.getValue();
                                        Object attributeValue = attributes.get(key);
                                        return (attributeValue != null
                                                && valuePredicate.test(attributeValue));
                                    });
                };
        this.predicate = this.predicate.or(spanRejecter);
        return this;
    }

    public SpanExporter build() {
        return new FilteringSpanExporter(delegate, this.predicate);
    }
}
