/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalAttributesSpanAppenderTest {

    @Mock private ReadWriteSpan span;

    private final GlobalAttributesSpanAppender globalAttributes =
            GlobalAttributesSpanAppender.create(Attributes.of(stringKey("key"), "value"));

    @Test
    void shouldAppendGlobalAttributes() {
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
}
