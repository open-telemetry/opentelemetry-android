/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.time.TestClock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.hours

internal class SessionManagerTest {
    @MockK
    lateinit var timeoutHandler: SessionIdTimeoutHandler

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { timeoutHandler.hasTimedOut() } returns false
        every { timeoutHandler.bump() } just Runs
    }

    @Test
    fun valueValid() {
        val sessionManager =
            SessionManager(
                TestClock.create(),
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = 4.hours,
            )
        val sessionId = sessionManager.getSessionId()
        assertThat(sessionId).isNotNull()
        assertThat(sessionId).hasSize(32)
        assertThat(Pattern.compile("[a-f0-9]+").matcher(sessionId).matches()).isTrue()
    }

    @Test
    fun valueSameUntil4Hours() {
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = 4.hours,
            )
        val value = sessionManager.getSessionId()
        assertThat(value).isEqualTo(sessionManager.getSessionId())
        clock.advance(3, TimeUnit.HOURS)
        assertThat(value).isEqualTo(sessionManager.getSessionId())
        clock.advance(59, TimeUnit.MINUTES)
        assertThat(value).isEqualTo(sessionManager.getSessionId())
        clock.advance(59, TimeUnit.SECONDS)
        assertThat(value).isEqualTo(sessionManager.getSessionId())

        // now it should change.
        clock.advance(1, TimeUnit.SECONDS)
        val newSessionId = sessionManager.getSessionId()
        assertThat(newSessionId).isNotNull()
        assertThat(value).isNotEqualTo(newSessionId)
    }

    @Test
    fun shouldCallSessionIdChangeListener() {
        val clock = TestClock.create()
        val observer = mockk<SessionObserver>()
        every { observer.onSessionStarted(any<Session>(), any<Session>()) } just Runs
        every { observer.onSessionEnded(any<Session>()) } just Runs

        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = 4.hours,
            )
        sessionManager.addObserver(observer)

        // The first call expires the Session.NONE initial session and notifies
        val firstSessionId = sessionManager.getSessionId()
        verify(exactly = 1) { timeoutHandler.bump() }
        verify(exactly = 0) { timeoutHandler.hasTimedOut() }
        verify(exactly = 1) { observer.onSessionStarted(any<Session>(), eq(Session.NONE)) }
        verify(exactly = 1) { observer.onSessionEnded(eq(Session.NONE)) }

        clock.advance(3, TimeUnit.HOURS)
        val secondSessionId = sessionManager.getSessionId()

        assertThat(firstSessionId).isEqualTo(secondSessionId)
        verify(exactly = 2) { timeoutHandler.bump() }
        verify(exactly = 1) { timeoutHandler.hasTimedOut() }
        verify(exactly = 1) { observer.onSessionStarted(any<Session>(), any<Session>()) }
        verify(exactly = 1) { observer.onSessionEnded(any<Session>()) }

        clock.advance(1, TimeUnit.HOURS)
        val thirdSessionId = sessionManager.getSessionId()

        verify(exactly = 3) { timeoutHandler.bump() }
        verify(exactly = 1) { timeoutHandler.hasTimedOut() }
        assertThat(thirdSessionId).isNotEqualTo(secondSessionId)
        verifyOrder {
            timeoutHandler.bump()
            observer.onSessionEnded(match { it.getId() == secondSessionId })
            observer.onSessionStarted(
                match { it.getId() == thirdSessionId },
                match { it.getId() == secondSessionId },
            )
        }
        confirmVerified(observer)
        confirmVerified(timeoutHandler)
    }

    @Test
    fun shouldCreateNewSessionIdAfterTimeout() {
        val sessionId =
            SessionManager(timeoutHandler = timeoutHandler, maxSessionLifetime = 4.hours)

        val value = sessionId.getSessionId()
        verify { timeoutHandler.bump() }

        assertThat(value).isEqualTo(sessionId.getSessionId())
        verify(exactly = 2) { timeoutHandler.bump() }

        every { timeoutHandler.hasTimedOut() } returns true

        assertThat(value).isNotEqualTo(sessionId.getSessionId())
        verify(exactly = 3) { timeoutHandler.bump() }
    }
}
