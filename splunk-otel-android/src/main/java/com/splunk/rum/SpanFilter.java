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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

final class SpanFilter implements SpanExporter {
    private final SpanExporter delegate;
    private final Predicate<String> rejectSpanNamesPredicate;
    private final Map<AttributeKey<?>, Predicate<?>> rejectSpanAttributesPredicates;
    private final Map<AttributeKey<?>, Function<?, ?>> spanAttributeReplacements;

    SpanFilter(SpanExporter delegate,
               Predicate<String> rejectSpanNamesPredicate,
               Map<AttributeKey<?>, Predicate<?>> rejectSpanAttributesPredicates,
               Map<AttributeKey<?>, Function<?, ?>> spanAttributeReplacements) {
        this.delegate = delegate;
        this.rejectSpanNamesPredicate = rejectSpanNamesPredicate;
        this.rejectSpanAttributesPredicates = rejectSpanAttributesPredicates;
        this.spanAttributeReplacements = spanAttributeReplacements;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        List<SpanData> filtered = new ArrayList<>();
        for (SpanData span : spans) {
            if (reject(span)) {
                continue;
            }
            filtered.add(modify(span));
        }
        return delegate.export(filtered);
    }

    private boolean reject(SpanData span) {
        if (rejectSpanNamesPredicate.test(span.getName())) {
            return true;
        }
        Attributes attributes = span.getAttributes();
        for (Map.Entry<AttributeKey<?>, Predicate<?>> e : rejectSpanAttributesPredicates.entrySet()) {
            AttributeKey<?> key = e.getKey();
            Predicate<? super Object> valuePredicate = (Predicate<? super Object>) e.getValue();
            Object attributeValue = attributes.get(key);
            if (attributeValue != null && valuePredicate.test(attributes.get(key))) {
                return true;
            }
        }
        return false;
    }

    private SpanData modify(SpanData span) {
        if (spanAttributeReplacements.isEmpty()) {
            return span;
        }

        AttributesBuilder modifiedAttributes = Attributes.builder();
        for (Map.Entry<AttributeKey<?>, Object> entry : span.getAttributes().asMap().entrySet()) {
            AttributeKey<?> key = entry.getKey();
            Function<? super Object, ?> valueModifier =
                    (Function<? super Object, ?>) spanAttributeReplacements.getOrDefault(key, Function.identity());
            Object newValue = valueModifier.apply(entry.getValue());
            if (newValue != null) {
                modifiedAttributes.put((AttributeKey<Object>) key, newValue);
            }
        }

        return new ModifiedSpanData(span, modifiedAttributes.build());
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }
}
