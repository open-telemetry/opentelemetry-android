/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.sdk.testing.time.TestClock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

class SessionIdTimeoutHandlerTest {
    @Test
    fun `user inactivity does not replace the background timeout`() {
        for (userTimeout in listOf(10.minutes, 60.minutes)) {
            val clock = TestClock.create()
            val handler = SessionIdTimeoutHandler(SessionConfig(userInactivityTimeout = userTimeout), clock)
            handler.bump()
            clock.advance(5, TimeUnit.MINUTES)
            handler.onApplicationBackgrounded()
            val remainingMinutes = if (userTimeout == 10.minutes) 5L else 15L
            clock.advance(remainingMinutes - 1, TimeUnit.MINUTES)
            handler.onApplicationBackgrounded()
            assertFalse(handler.hasTimedOut())
            clock.advance(1, TimeUnit.MINUTES)
            assertTrue(handler.hasTimedOut())
            handler.onApplicationForegrounded()
            assertTrue(handler.hasTimedOut())
            assertFalse(handler.bumpIfActive())
            handler.bump()
            assertFalse(handler.hasTimedOut())
        }
    }

    @Test
    fun `active input preserves the latest timestamp without reviving expiry`() {
        val clock = TestClock.create()
        val handler = SessionIdTimeoutHandler(SessionConfig(userInactivityTimeout = 1.minutes), clock)
        handler.bump()
        repeat(120) {
            clock.advance(16, TimeUnit.MILLISECONDS)
            assertTrue(handler.bumpIfActive())
        }
        clock.advance(59999, TimeUnit.MILLISECONDS)
        assertFalse(handler.hasTimedOut())
        clock.advance(1, TimeUnit.MILLISECONDS)
        assertFalse(handler.bumpIfActive())
        assertTrue(handler.hasTimedOut())
        handler.onApplicationForegrounded()
        assertFalse(handler.bumpIfActive())
    }

    @Test
    fun `background timeout begins on background entry not the last input`() {
        val clock = TestClock.create()
        val handler = SessionIdTimeoutHandler(SessionConfig(userInactivityTimeout = 60.minutes), clock)
        handler.bump()
        clock.advance(10, TimeUnit.MINUTES)
        handler.onApplicationBackgrounded()
        clock.advance(14, TimeUnit.MINUTES)
        assertFalse(handler.hasTimedOut())
        clock.advance(1, TimeUnit.MINUTES)
        assertTrue(handler.hasTimedOut())
    }

    @Test
    fun `synchronizing on the handler does not block timeout updates`() {
        val timeoutHandler = SessionIdTimeoutHandler(TestClock.create(), 5.nanoseconds)
        val executor = Executors.newSingleThreadExecutor()
        try {
            synchronized(timeoutHandler) {
                val update =
                    executor.submit<Boolean> {
                        timeoutHandler.onApplicationBackgrounded()
                        timeoutHandler.bump()
                        timeoutHandler.onApplicationForegrounded()
                        timeoutHandler.hasTimedOut()
                    }
                assertThat(update.get(5, TimeUnit.SECONDS)).isFalse()
            }
        } finally {
            executor.shutdownNow()
        }
    }

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

        timeoutHandler.onApplicationBackgrounded()
        clock.advance(14, TimeUnit.MINUTES)
        timeoutHandler.onApplicationForegrounded()
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
    fun shouldPreserveBackgroundExpiryOnForegroundReturn() {
        val clock: TestClock = TestClock.create()
        val timeoutHandler =
            SessionIdTimeoutHandler(clock, SessionConfig.withDefaults().backgroundInactivityTimeout)

        timeoutHandler.onApplicationBackgrounded()
        timeoutHandler.bump()

        // Expiry while backgrounded survives the return to foreground.
        clock.advance(20, TimeUnit.MINUTES)
        timeoutHandler.onApplicationForegrounded()
        assertTrue(timeoutHandler.hasTimedOut())
        timeoutHandler.bump()

        // Creating a new session clears the pending expiry.
        clock.advance(Duration.ofHours(4))
        assertFalse(timeoutHandler.hasTimedOut())
    }

    @Test
    fun shouldPreserveCustomBackgroundExpiryOnForegroundReturn() {
        val clock: TestClock = TestClock.create()
        val timeoutHandler =
            SessionIdTimeoutHandler(clock, 5.nanoseconds)

        timeoutHandler.onApplicationBackgrounded()
        timeoutHandler.bump()

        // Expiry while backgrounded survives the return to foreground.
        clock.advance(6, TimeUnit.MINUTES)
        timeoutHandler.onApplicationForegrounded()
        assertTrue(timeoutHandler.hasTimedOut())
        timeoutHandler.bump()

        // Creating a new session clears the pending expiry.
        clock.advance(Duration.ofHours(4))
        assertFalse(timeoutHandler.hasTimedOut())
    }
}
