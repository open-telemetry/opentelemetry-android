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

import static org.junit.Assert.*;

import io.opentelemetry.sdk.testing.time.TestClock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class SessionIdTimeoutHandlerTest {

    @Test
    public void shouldNeverTimeOutInForeground() {
        TestClock clock = TestClock.create();
        SessionIdTimeoutHandler timeoutHandler = new SessionIdTimeoutHandler(clock);

        assertFalse(timeoutHandler.hasTimedOut());
        timeoutHandler.bump();

        // never time out in foreground
        clock.advance(Duration.ofHours(4));
        assertFalse(timeoutHandler.hasTimedOut());
    }

    @Test
    public void shouldApply15MinutesTimeoutToAppsInBackground() {
        TestClock clock = TestClock.create();
        SessionIdTimeoutHandler timeoutHandler = new SessionIdTimeoutHandler(clock);

        timeoutHandler.appBackgrounded();
        timeoutHandler.bump();

        assertFalse(timeoutHandler.hasTimedOut());
        timeoutHandler.bump();

        // do not timeout if <15 minutes have passed
        clock.advance(14, TimeUnit.MINUTES);
        clock.advance(59, TimeUnit.SECONDS);
        assertFalse(timeoutHandler.hasTimedOut());
        timeoutHandler.bump();

        // restart the timeout counter after bump()
        clock.advance(1, TimeUnit.MINUTES);
        assertFalse(timeoutHandler.hasTimedOut());
        timeoutHandler.bump();

        // timeout after 15 minutes
        clock.advance(15, TimeUnit.MINUTES);
        assertTrue(timeoutHandler.hasTimedOut());

        // bump() resets the counter
        timeoutHandler.bump();
        assertFalse(timeoutHandler.hasTimedOut());
    }

    @Test
    public void shouldApplyTimeoutToFirstSpanAfterAppBeingMovedToForeground() {
        TestClock clock = TestClock.create();
        SessionIdTimeoutHandler timeoutHandler = new SessionIdTimeoutHandler(clock);

        timeoutHandler.appBackgrounded();
        timeoutHandler.bump();

        // the first span after app is moved to the foreground gets timed out
        timeoutHandler.appForegrounded();
        clock.advance(20, TimeUnit.MINUTES);
        assertTrue(timeoutHandler.hasTimedOut());
        timeoutHandler.bump();

        // after the initial span it's the same as the usual foreground scenario
        clock.advance(Duration.ofHours(4));
        assertFalse(timeoutHandler.hasTimedOut());
    }
}
