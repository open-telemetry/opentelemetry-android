/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalAttributesSpanAppenderTest {

    @Mock private ReadWriteSpan span;

    @Test
    void shouldAppendGlobalAttributes() {
        GlobalAttributesSpanAppender globalAttributes =
                GlobalAttributesSpanAppender.create(Attributes.of(stringKey("key"), "value"));
        globalAttributes.update(attributesBuilder -> attributesBuilder.put("key", "value2"));
        globalAttributes.update(
                attributesBuilder -> attributesBuilder.put(longKey("otherKey"), 1234L));

        assertTrue(globalAttributes.isStartRequired());
        globalAttributes.onStart(Context.root(), span);

        verify(span)
                .setAllAttributes(
                        Attributes.of(stringKey("key"), "value2", longKey("otherKey"), 1234L));

        assertFalse(globalAttributes.isEndRequired());
    }

    @Test
    void createWithSupplier() {
        Attributes attrs = Attributes.of(stringKey("foo"), "bar");
        GlobalAttributesSpanAppender globalAttributes =
                GlobalAttributesSpanAppender.create(() -> attrs);

        globalAttributes.onStart(Context.root(), span);
        verify(span).setAllAttributes(Attributes.of(stringKey("foo"), "bar"));
    }

    @Test
    void updateWithSupplierReplacesSupplier() {
        Attributes attrs = Attributes.of(stringKey("foo"), "bar");
        Supplier<Attributes> originalSupplier = () -> fail("Should not have been called");

        GlobalAttributesSpanAppender globalAttributes =
                GlobalAttributesSpanAppender.create(originalSupplier);
        globalAttributes.update(() -> attrs);

        globalAttributes.onStart(Context.root(), span);
        verify(span).setAllAttributes(Attributes.of(stringKey("foo"), "bar"));
    }

    @Test
    void updateWithAttributesReplacesSupplier() {
        Attributes attrs = Attributes.of(stringKey("foo"), "bar");
        Attributes extra = Attributes.of(stringKey("bar"), "baz");
        Supplier<Attributes> originalSupplier = mock(Supplier.class);

        when(originalSupplier.get())
                .thenReturn(attrs)
                .thenThrow(new RuntimeException("Should not have been called again."));

        GlobalAttributesSpanAppender globalAttributes =
                GlobalAttributesSpanAppender.create(originalSupplier);
        globalAttributes.update(builder -> builder.putAll(extra));

        globalAttributes.onStart(Context.root(), span);
        verify(span)
                .setAllAttributes(Attributes.of(stringKey("foo"), "bar", stringKey("bar"), "baz"));
    }
}
