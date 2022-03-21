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

package com.splunk.rum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.opentelemetry.sdk.testing.time.TestClock;

@RunWith(MockitoJUnitRunner.class)
public class SessionIdTest {

    @Mock
    SessionIdTimeoutHandler timeoutHandler;

    @Test
    public void valueValid() {
        String sessionId = new SessionId(TestClock.create(), timeoutHandler).getSessionId();
        assertNotNull(sessionId);
        assertEquals(32, sessionId.length());
        assertTrue(Pattern.compile("[a-f0-9]+").matcher(sessionId).matches());
    }

    @Test
    public void valueSameUntil4Hours() {
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

        //now it should change.
        clock.advance(1, TimeUnit.SECONDS);
        String newSessionId = sessionId.getSessionId();
        assertNotNull(newSessionId);
        assertNotEquals(value, newSessionId);
    }

    @Test
    public void shouldCallSessionIdChangeListener() {
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
    public void shouldCreateNewSessionIdAfterTimeout() {
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