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

import static io.opentelemetry.rum.internal.RumConstants.SESSION_ID_KEY;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionIdSpanAppenderTest {

    @Mock SessionId sessionId;
    @Mock ReadWriteSpan span;

    @Test
    void shouldSetSessionIdAsSpanAttribute() {
        when(sessionId.getSessionId()).thenReturn("42");

        SessionIdSpanAppender underTest = new SessionIdSpanAppender(sessionId);

        assertTrue(underTest.isStartRequired());
        underTest.onStart(Context.root(), span);

        verify(span).setAttribute(SESSION_ID_KEY, "42");

        assertFalse(underTest.isEndRequired());
    }
}
