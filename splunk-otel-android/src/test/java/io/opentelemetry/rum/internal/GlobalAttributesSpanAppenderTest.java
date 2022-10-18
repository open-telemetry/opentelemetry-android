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

package io.opentelemetry.rum.internal;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.Test;
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
