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

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.opentelemetry.sdk.internal.TestClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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