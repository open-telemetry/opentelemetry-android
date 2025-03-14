/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.session

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

internal class SessionManagerImplTest {
    @MockK
    lateinit var timeoutHandler: SessionBackgroundTimeoutHandler

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { timeoutHandler.hasTimedOut() } returns false
        every { timeoutHandler.bump() } just Runs
    }

    @Test
    fun valueValid() {
        val sessionManager =
            SessionManagerImpl(
                TestClock.create(),
                timeoutHandler = timeoutHandler,
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
            SessionManagerImpl(
                clock,
                timeoutHandler = timeoutHandler,
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

        // The initialization of SessionManagerImpl creates the first session for the application
        // No observer is available on application start when first session is created
        val sessionManager =
            SessionManagerImpl(
                clock,
                timeoutHandler = timeoutHandler,
            )

        verify(exactly = 1) { timeoutHandler.bump() }
        verify(exactly = 0) { timeoutHandler.hasTimedOut() }
        verify(exactly = 0) { observer.onSessionStarted(any<Session>(), eq(Session.NONE)) }
        verify(exactly = 0) { observer.onSessionEnded(eq(Session.NONE)) }

        // Add session change observer
        sessionManager.addObserver(observer)

        // Fetch the first session's id
        val firstSessionId = sessionManager.getSessionId()

        clock.advance(3, TimeUnit.HOURS)
        val firstSessionIdContinued = sessionManager.getSessionId()

        assertThat(firstSessionId).isEqualTo(firstSessionIdContinued)
        verify(exactly = 3) { timeoutHandler.bump() }
        verify(exactly = 2) { timeoutHandler.hasTimedOut() }
        verify(exactly = 0) { observer.onSessionStarted(any<Session>(), any<Session>()) }
        verify(exactly = 0) { observer.onSessionEnded(any<Session>()) }

        clock.advance(1, TimeUnit.HOURS)
        val secondSessionId = sessionManager.getSessionId()

        verify(exactly = 4) { timeoutHandler.bump() }
        verify(exactly = 2) { timeoutHandler.hasTimedOut() }
        assertThat(secondSessionId).isNotEqualTo(firstSessionId)
        verifyOrder {
            timeoutHandler.bump()
            observer.onSessionEnded(match { it.getId() == firstSessionId })
            observer.onSessionStarted(
                match { it.getId() == secondSessionId },
                match { it.getId() == firstSessionId },
            )
        }
        confirmVerified(observer)
        confirmVerified(timeoutHandler)
    }

    @Test
    fun shouldCreateNewSessionIdAfterTimeout() {
        val sessionManager = SessionManagerImpl(timeoutHandler = timeoutHandler)
        verify { timeoutHandler.bump() }

        val firstSessionId = sessionManager.getSessionId()
        verify { timeoutHandler.bump() }

        assertThat(firstSessionId).isEqualTo(sessionManager.getSessionId())
        verify(exactly = 3) { timeoutHandler.bump() }

        every { timeoutHandler.hasTimedOut() } returns true

        assertThat(firstSessionId).isNotEqualTo(sessionManager.getSessionId())
        verify(exactly = 4) { timeoutHandler.bump() }
    }
}
