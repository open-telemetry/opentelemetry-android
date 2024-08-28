/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.opentelemetry.android.common.RumConstants.SESSION_ID_KEY;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.opentelemetry.android.session.SessionManager;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;

@ExtendWith(MockitoExtension.class)
class SessionIdSpanAppenderTest {

    @Mock
    SessionManager sessionManager;
    @Mock ReadWriteSpan span;

    @Test
    void shouldSetSessionIdAsSpanAttribute() {
        when(sessionManager.getSessionId()).thenReturn("42");

        SessionIdSpanAppender underTest = new SessionIdSpanAppender(sessionManager);

        assertTrue(underTest.isStartRequired());
        underTest.onStart(Context.root(), span);

        verify(span).setAttribute(SESSION_ID_KEY, "42");

        assertFalse(underTest.isEndRequired());
    }
}
