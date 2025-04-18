/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A [SpanProcessor] implementation that appends a set of [attributes][Attributes]
 * to every span. The attributes are supplied via Supplier. This Supplier may alter
 * its results and return different attributes over time. collection is mutable, and can be updated
 * by calling [.update].
 */
internal class GlobalAttributesSpanAppender(
    initialState: Supplier<Attributes>,
) : SpanProcessor {
    private val attributesSupplier = AtomicReference(initialState)

    override fun onStart(
        parentContext: Context,
        span: ReadWriteSpan,
    ) {
        span.setAllAttributes(attributes)
    }

    private val attributes: Attributes
        get() = attributesSupplier.get().get()

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {}

    override fun isEndRequired(): Boolean = false

    /**
     * Update the global set of attributes to be appended to every span.
     *
     * Note: Calling this method invalidates the Supplier originally passed to this [ ] and any other previously updated Supplier.
     *
     * @param attributesUpdater A function which will update the current set of attributes, by
     * operating on a [AttributesBuilder] from the current set.
     */
    fun update(attributesUpdater: Consumer<AttributesBuilder>) {
        synchronized(attributesSupplier) {
            val oldAttributes = attributes
            val builder = oldAttributes.toBuilder()
            attributesUpdater.accept(builder)
            val newAttributes = builder.build()
            attributesSupplier.set(Supplier { newAttributes })
        }
    }

    /**
     * Replaces the currently configured attributes Supplier with a new one.
     *
     * @param attributesSupplier Supplier to call to obtain Attributes for every span.
     */
    fun update(attributesSupplier: Supplier<Attributes>) {
        this.attributesSupplier.set(attributesSupplier)
    }
}
