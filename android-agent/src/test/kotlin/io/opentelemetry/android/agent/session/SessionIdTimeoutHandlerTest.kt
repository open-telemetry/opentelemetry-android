/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.sdk.testing.time.TestClock
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.nanoseconds

class SessionIdTimeoutHandlerTest {
    @Test
    fun `times out at the default inactivity boundary`() {
        val clock = TestClock.create()
        val timeoutHandler =
            SessionIdTimeoutHandler(clock, SessionConfig.withDefaults().inactivityTimeout)

        clock.advance(14, TimeUnit.MINUTES)
        clock.advance(59, TimeUnit.SECONDS)
        assertFalse(timeoutHandler.hasTimedOut())

        clock.advance(1, TimeUnit.SECONDS)
        assertTrue(timeoutHandler.hasTimedOut())
    }

    @Test
    fun `meaningful activity restarts the inactivity window`() {
        val clock = TestClock.create()
        val timeoutHandler =
            SessionIdTimeoutHandler(clock, SessionConfig.withDefaults().inactivityTimeout)

        clock.advance(10, TimeUnit.MINUTES)
        timeoutHandler.bump()
        clock.advance(10, TimeUnit.MINUTES)
        assertFalse(timeoutHandler.hasTimedOut())

        clock.advance(5, TimeUnit.MINUTES)
        assertTrue(timeoutHandler.hasTimedOut())
    }

    @Test
    fun `custom inactivity timeout uses exact boundary`() {
        val clock = TestClock.create()
        val timeoutHandler =
            SessionIdTimeoutHandler(clock, 5.nanoseconds)

        clock.advance(4, TimeUnit.NANOSECONDS)
        assertFalse(timeoutHandler.hasTimedOut())

        clock.advance(1, TimeUnit.NANOSECONDS)
        assertTrue(timeoutHandler.hasTimedOut())
    }
}
