/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.sdk.testing.time.TestClock
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.nanoseconds

class SessionIdTimeoutHandlerTest {
    @Test
    fun shouldNeverTimeOutInForeground() {
        val clock: TestClock = TestClock.create()
        val timeoutHandler =
            SessionIdTimeoutHandler(clock, SessionConfig.withDefaults().backgroundInactivityTimeout)

        assertFalse(timeoutHandler.hasTimedOut())
        timeoutHandler.bump()

        // never time out in foreground
        clock.advance(Duration.ofHours(4))
        assertFalse(timeoutHandler.hasTimedOut())
    }

    @Test
    fun shouldApply15MinutesTimeoutToAppsInBackground() {
        val clock: TestClock = TestClock.create()
        val timeoutHandler =
            SessionIdTimeoutHandler(clock, SessionConfig.withDefaults().backgroundInactivityTimeout)

        timeoutHandler.onApplicationBackgrounded()
        timeoutHandler.bump()

        assertFalse(timeoutHandler.hasTimedOut())
        timeoutHandler.bump()

        // do not timeout if <15 minutes have passed
        clock.advance(14, TimeUnit.MINUTES)
        clock.advance(59, TimeUnit.SECONDS)
        assertFalse(timeoutHandler.hasTimedOut())
        timeoutHandler.bump()

        // restart the timeout counter after bump()
        clock.advance(1, TimeUnit.MINUTES)
        assertFalse(timeoutHandler.hasTimedOut())
        timeoutHandler.bump()

        // timeout after 15 minutes
        clock.advance(15, TimeUnit.MINUTES)
        assertTrue(timeoutHandler.hasTimedOut())

        // bump() resets the counter
        timeoutHandler.bump()
        assertFalse(timeoutHandler.hasTimedOut())
    }

    @Test
    fun shouldApplyTimeoutToFirstSpanAfterAppBeingMovedToForeground() {
        val clock: TestClock = TestClock.create()
        val timeoutHandler =
            SessionIdTimeoutHandler(clock, SessionConfig.withDefaults().backgroundInactivityTimeout)

        timeoutHandler.onApplicationBackgrounded()
        timeoutHandler.bump()

        // the first span after app is moved to the foreground gets timed out
        timeoutHandler.onApplicationForegrounded()
        clock.advance(20, TimeUnit.MINUTES)
        assertTrue(timeoutHandler.hasTimedOut())
        timeoutHandler.bump()

        // after the initial span it's the same as the usual foreground scenario
        clock.advance(Duration.ofHours(4))
        assertFalse(timeoutHandler.hasTimedOut())
    }

    @Test
    fun shouldApplyCustomTimeoutToFirstSpanAfterAppBeingMovedToForeground() {
        val clock: TestClock = TestClock.create()
        val timeoutHandler =
            SessionIdTimeoutHandler(clock, 5.nanoseconds)

        timeoutHandler.onApplicationBackgrounded()
        timeoutHandler.bump()

        // the first span after app is moved to the foreground gets timed out
        timeoutHandler.onApplicationForegrounded()
        clock.advance(6, TimeUnit.MINUTES)
        assertTrue(timeoutHandler.hasTimedOut())
        timeoutHandler.bump()

        // after the initial span it's the same as the usual foreground scenario
        clock.advance(Duration.ofHours(4))
        assertFalse(timeoutHandler.hasTimedOut())
    }
}
