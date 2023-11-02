/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A {@link SpanProcessor} implementation that appends a set of {@linkplain Attributes attributes}
 * to every span that is exported. The attributes are supplied via Supplier. This Supplier may alter
 * its results and return different attributes over time. collection is mutable, and can be updated
 * by calling {@link #update(Consumer)}.
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
        return create(() -> initialState);
    }

    /**
     * Returns a new {@link GlobalAttributesSpanAppender} which calls the given supplier to populate
     * the global attributes;
     *
     * @param attributeSupplier a Supplier of Attributes to be placed on every span.
     */
    public static GlobalAttributesSpanAppender create(Supplier<Attributes> attributeSupplier) {
        return new GlobalAttributesSpanAppender(attributeSupplier);
    }

    private final AtomicReference<Supplier<Attributes>> attributesSupplier;

    private GlobalAttributesSpanAppender(Supplier<Attributes> initialState) {
        this.attributesSupplier = new AtomicReference<>(initialState);
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        span.setAllAttributes(getAttributes());
    }

    private Attributes getAttributes() {
        Supplier<Attributes> supplier = attributesSupplier.get();
        if (supplier != null) {
            Attributes result = supplier.get();
            if (result != null) {
                return result;
            }
        }
        return Attributes.empty();
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
     * Update the global set of attributes to be appended to every span.
     *
     * <p>Note: Calling this method invalidates the Supplier originally passed to this {@link
     * GlobalAttributesSpanAppender} and any other previously updated Supplier.
     *
     * @param attributesUpdater A function which will update the current set of attributes, by
     *     operating on a {@link AttributesBuilder} from the current set.
     */
    public void update(Consumer<AttributesBuilder> attributesUpdater) {
        synchronized (attributesSupplier) {
            Attributes oldAttributes = getAttributes();

            AttributesBuilder builder = oldAttributes.toBuilder();
            attributesUpdater.accept(builder);
            Attributes newAttributes = builder.build();

            attributesSupplier.set(() -> newAttributes);
        }
    }

    /**
     * Replaces the currently configured attributes Supplier with a new one.
     *
     * @param attributesSupplier Supplier to call to obtain Attributes for every span.
     */
    public void update(Supplier<Attributes> attributesSupplier) {
        this.attributesSupplier.set(attributesSupplier);
    }
}
