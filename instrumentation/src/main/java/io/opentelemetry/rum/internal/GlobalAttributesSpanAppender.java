/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A {@link SpanProcessor} implementation that appends a set of {@linkplain Attributes attributes}
 * to every span that is exported. The attributes collection is mutable, and can be updated by
 * calling {@link #update(Consumer)}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class GlobalAttributesSpanAppender implements SpanProcessor {

    /**
     * Returns a new {@link GlobalAttributesSpanAppender} with a given initial attributes.
     *
     * @param initialState The initial collection of attributes to append to every span.
     */
    public static GlobalAttributesSpanAppender create(Attributes initialState) {
        return new GlobalAttributesSpanAppender(initialState);
    }

    private final AtomicReference<Attributes> attributes;

    private GlobalAttributesSpanAppender(Attributes initialState) {
        this.attributes = new AtomicReference<>(initialState);
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        span.setAllAttributes(attributes.get());
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {}

    @Override
    public boolean isEndRequired() {
        return false;
    }

    /**
     * Update the global set of attributes that will be appended to every span.
     *
     * <p>Note: this operation performs an atomic update. The passed function should be free from
     * side effects, since it may be called multiple times in case of thread contention.
     *
     * @param attributesUpdater A function which will update the current set of attributes, by
     *     operating on a {@link AttributesBuilder} from the current set.
     */
    public void update(Consumer<AttributesBuilder> attributesUpdater) {
        while (true) {
            // we're absolutely certain this will never be null
            Attributes oldAttributes = requireNonNull(attributes.get());

            AttributesBuilder builder = oldAttributes.toBuilder();
            attributesUpdater.accept(builder);
            Attributes newAttributes = builder.build();

            if (attributes.compareAndSet(oldAttributes, newAttributes)) {
                break;
            }
        }
    }
}
