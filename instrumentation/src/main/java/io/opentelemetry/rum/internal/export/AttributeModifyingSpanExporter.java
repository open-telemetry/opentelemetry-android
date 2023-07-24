/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
