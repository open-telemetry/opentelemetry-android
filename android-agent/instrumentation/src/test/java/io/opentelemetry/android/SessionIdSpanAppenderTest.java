/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.RumConstants.SESSION_ID_KEY;
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
