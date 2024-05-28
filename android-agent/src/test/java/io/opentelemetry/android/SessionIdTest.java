/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.testing.time.TestClock;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionIdTest {

    @Mock SessionIdTimeoutHandler timeoutHandler;

    @Test
    void valueValid() {
        String sessionId = new SessionId(TestClock.create(), timeoutHandler).getSessionId();
        assertNotNull(sessionId);
        assertEquals(32, sessionId.length());
        assertTrue(Pattern.compile("[a-f0-9]+").matcher(sessionId).matches());
    }

    @Test
    void valueSameUntil4Hours() {
        TestClock clock = TestClock.create();
        SessionId sessionId = new SessionId(clock, timeoutHandler);
        String value = sessionId.getSessionId();
        assertEquals(value, sessionId.getSessionId());
        clock.advance(3, TimeUnit.HOURS);
        assertEquals(value, sessionId.getSessionId());
        clock.advance(59, TimeUnit.MINUTES);
        assertEquals(value, sessionId.getSessionId());
        clock.advance(59, TimeUnit.SECONDS);
        assertEquals(value, sessionId.getSessionId());

        // now it should change.
        clock.advance(1, TimeUnit.SECONDS);
        String newSessionId = sessionId.getSessionId();
        assertNotNull(newSessionId);
        assertNotEquals(value, newSessionId);
    }

    @Test
    void shouldCallSessionIdChangeListener() {
        TestClock clock = TestClock.create();
        SessionIdChangeListener listener = mock(SessionIdChangeListener.class);
        SessionId sessionId = new SessionId(clock, timeoutHandler);
        sessionId.setSessionIdChangeListener(listener);

        String firstSessionId = sessionId.getSessionId();
        clock.advance(3, TimeUnit.HOURS);
        sessionId.getSessionId();
        verify(timeoutHandler, times(2)).bump();
        verify(listener, never()).onChange(anyString(), anyString());

        clock.advance(1, TimeUnit.HOURS);
        String secondSessionId = sessionId.getSessionId();
        InOrder io = inOrder(timeoutHandler, listener);
        io.verify(timeoutHandler).bump();
        io.verify(listener).onChange(firstSessionId, secondSessionId);
        io.verifyNoMoreInteractions();
    }

    @Test
    void shouldCreateNewSessionIdAfterTimeout() {
        SessionId sessionId = new SessionId(timeoutHandler);

        String value = sessionId.getSessionId();
        verify(timeoutHandler).bump();

        assertEquals(value, sessionId.getSessionId());
        verify(timeoutHandler, times(2)).bump();

        when(timeoutHandler.hasTimedOut()).thenReturn(true);

        assertNotEquals(value, sessionId.getSessionId());
        verify(timeoutHandler, times(3)).bump();
    }
}
