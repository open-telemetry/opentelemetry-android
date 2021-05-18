package com.splunk.rum;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.opentelemetry.sdk.internal.TestClock;

import static org.junit.Assert.*;

public class SessionIdTest {

    @Test
    public void valueValid() {
        String sessionId = new SessionId(TestClock.create()).getSessionId();
        assertNotNull(sessionId);
        assertEquals(32, sessionId.length());
        assertTrue(Pattern.compile("[a-f0-9]+").matcher(sessionId).matches());
    }

    @Test
    public void valueSameUntil4Hours() {
        TestClock clock = TestClock.create();
        SessionId sessionId = new SessionId(clock);
        String value = sessionId.getSessionId();
        assertEquals(value, sessionId.getSessionId());
        clock.advanceMillis(TimeUnit.HOURS.toMillis(3));
        assertEquals(value, sessionId.getSessionId());
        clock.advanceMillis(TimeUnit.MINUTES.toMillis(59));
        assertEquals(value, sessionId.getSessionId());
        clock.advanceMillis(TimeUnit.SECONDS.toMillis(59));
        assertEquals(value, sessionId.getSessionId());

        //now it should change.
        clock.advanceMillis(TimeUnit.SECONDS.toMillis(1));
        String newSessionId = sessionId.getSessionId();
        assertNotNull(newSessionId);
        assertNotEquals(value, newSessionId);
    }
}