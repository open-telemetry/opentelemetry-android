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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A SpanExporter that is configured to modify some of its attributes at export time. */
public class AttributeModifyingSpanExporter implements SpanExporter {

    private final SpanExporter delegate;
    private final Map<AttributeKey<?>, Function<?, ?>> spanAttributeReplacements;

    public AttributeModifyingSpanExporter(
            SpanExporter delegate, Map<AttributeKey<?>, Function<?, ?>> spanAttributeReplacements) {
        this.delegate = delegate;
        this.spanAttributeReplacements = spanAttributeReplacements;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (spanAttributeReplacements.isEmpty()) {
            return delegate.export(spans);
        }

        List<SpanData> modifiedSpans =
                spans.stream().map(this::doModify).collect(Collectors.toList());
        return delegate.export(modifiedSpans);
    }

    private SpanData doModify(SpanData span) {
        Attributes modifiedAttributes = buildModifiedAttributes(span);
        return new ModifiedSpanData(span, modifiedAttributes);
    }

    @NonNull
    private Attributes buildModifiedAttributes(SpanData span) {
        AttributesBuilder modifiedAttributes = Attributes.builder();
        span.getAttributes()
                .forEach(
                        (key, attrValue) -> {
                            Function<? super Object, ?> remapper = getRemapper(key);
                            if (remapper != null) {
                                attrValue = remapper.apply(attrValue);
                            }
                            if (attrValue != null) {
                                modifiedAttributes.put((AttributeKey<Object>) key, attrValue);
                            }
                        });
        return modifiedAttributes.build();
    }

    @Nullable
    private Function<? super Object, ?> getRemapper(AttributeKey<?> key) {
        return (Function<? super Object, ?>) spanAttributeReplacements.get(key);
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
