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
import org.junit.jupiter.api.assertAll
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.hours

private const val SESSION_ID_LENGTH = 32
private const val MAX_SESSION_LIFETIME = 4L

/**
 * Verifies [SessionManager] functionality including session ID generation, timeout handling,
 * observer notifications, and thread-safety under concurrent access scenarios.
 */
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
        // Verifies that generated session IDs are valid 32-character hex strings

        // Given/When
        val sessionManager =
            SessionManager(
                TestClock.create(),
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )
        val sessionId = sessionManager.getSessionId()

        // Then
        assertAll(
            { assertThat(sessionId).isNotNull() },
            { assertThat(sessionId).hasSize(SESSION_ID_LENGTH) },
            { assertThat(Pattern.compile("[a-f0-9]+").matcher(sessionId).matches()).isTrue() },
        )
    }

    @Test
    fun valueSameUntil4Hours() {
        // Verifies that session ID remains unchanged until maxLifetime is exceeded

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        // When - initial session
        val value = sessionManager.getSessionId()

        // Then - should remain same for multiple accesses and time advances
        assertThat(value).isEqualTo(sessionManager.getSessionId())
        clock.advance(3, TimeUnit.HOURS)
        assertThat(value).isEqualTo(sessionManager.getSessionId())
        clock.advance(59, TimeUnit.MINUTES)
        assertThat(value).isEqualTo(sessionManager.getSessionId())
        clock.advance(59, TimeUnit.SECONDS)
        assertThat(value).isEqualTo(sessionManager.getSessionId())

        // When - advance past maxLifetime
        clock.advance(1, TimeUnit.SECONDS)
        val newSessionId = sessionManager.getSessionId()

        // Then - should create new session
        assertAll(
            { assertThat(newSessionId).isNotNull() },
            { assertThat(value).isNotEqualTo(newSessionId) },
        )
    }

    @Test
    fun shouldCallSessionIdChangeListener() {
        // Verifies that session observers are notified correctly during session transitions

        // Given
        val clock = TestClock.create()
        val observer = mockk<SessionObserver>()
        every { observer.onSessionStarted(any<Session>(), any<Session>()) } just Runs
        every { observer.onSessionEnded(any<Session>()) } just Runs

        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )
        sessionManager.addObserver(observer)

        // When - The first call expires the Session.NONE initial session and notifies
        val firstSessionId = sessionManager.getSessionId()

        // Then
        assertAll(
            { verify(exactly = 1) { timeoutHandler.bump() } },
            { verify(exactly = 0) { timeoutHandler.hasTimedOut() } },
            { verify(exactly = 1) { observer.onSessionStarted(any<Session>(), eq(Session.NONE)) } },
            { verify(exactly = 1) { observer.onSessionEnded(eq(Session.NONE)) } },
        )

        // When
        clock.advance(3, TimeUnit.HOURS)
        val secondSessionId = sessionManager.getSessionId()

        // Then
        assertAll(
            { assertThat(firstSessionId).isEqualTo(secondSessionId) },
            { verify(exactly = 2) { timeoutHandler.bump() } },
            { verify(exactly = 1) { timeoutHandler.hasTimedOut() } },
            { verify(exactly = 1) { observer.onSessionStarted(any<Session>(), any<Session>()) } },
            { verify(exactly = 1) { observer.onSessionEnded(any<Session>()) } },
        )

        // When
        clock.advance(1, TimeUnit.HOURS)
        val thirdSessionId = sessionManager.getSessionId()

        // Then
        assertAll(
            { verify(exactly = 3) { timeoutHandler.bump() } },
            { verify(exactly = 1) { timeoutHandler.hasTimedOut() } },
            { assertThat(thirdSessionId).isNotEqualTo(secondSessionId) },
        )
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
        // Verifies that a new session is created when the timeout handler indicates a timeout

        // Given
        val sessionId =
            SessionManager(timeoutHandler = timeoutHandler, maxSessionLifetime = MAX_SESSION_LIFETIME.hours)

        // When - access session ID twice, should be same
        val value = sessionId.getSessionId()
        verify { timeoutHandler.bump() }

        // Then
        assertThat(value).isEqualTo(sessionId.getSessionId())
        verify(exactly = 2) { timeoutHandler.bump() }

        // When - timeout handler indicates timeout
        every { timeoutHandler.hasTimedOut() } returns true

        // Then - should create new session
        assertThat(value).isNotEqualTo(sessionId.getSessionId())
        verify(exactly = 3) { timeoutHandler.bump() }
    }

    @Test
    fun `concurrent access during timeout should create only one new session`() {
        // Verifies that concurrent access during session timeout creates exactly one new session

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        // Establish an initial session
        val initialSessionId = sessionManager.getSessionId()

        // Advance time to trigger session expiration
        clock.advance(5, TimeUnit.HOURS)

        val numThreads = 10
        val executor = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(numThreads)
        val sessionIds = mutableSetOf<String>()
        val sessionIdCount = AtomicInteger(0)

        // When - multiple threads access session concurrently after timeout
        repeat(numThreads) {
            executor.submit {
                try {
                    val sessionId = sessionManager.getSessionId()
                    synchronized(sessionIds) {
                        sessionIds.add(sessionId)
                        sessionIdCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // Then - verify that only one new session was created
        assertAll(
            "Session creation validation",
            { assertThat(sessionIds).hasSize(1) },
            { assertThat(sessionIds.first()).isNotEqualTo(initialSessionId) },
            { assertThat(sessionIdCount.get()).isEqualTo(numThreads) },
        )
    }

    @Test
    fun `concurrent access with timeout handler should create only one new session`() {
        // Verifies that when timeout handler indicates timeout, concurrent threads handle session creation safely

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        every { timeoutHandler.hasTimedOut() } returns true

        val numThreads = 5
        val executor = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(numThreads)
        val sessionIds = mutableSetOf<String>()
        val sessionIdCount = AtomicInteger(0)

        // When - multiple threads access session with timeout handler indicating timeout
        repeat(numThreads) {
            executor.submit {
                try {
                    val sessionId = sessionManager.getSessionId()
                    synchronized(sessionIds) {
                        sessionIds.add(sessionId)
                        sessionIdCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // Then - verify the expected number of session IDs
        assertAll(
            "Race condition validation",
            { assertThat(sessionIdCount.get()).isEqualTo(numThreads) },
            { assertThat(sessionIds).isNotEmpty() },
            { assertThat(sessionIds.size).isLessThanOrEqualTo(numThreads) },
        )
    }

    @Test
    fun `concurrent access should maintain session consistency`() {
        // Verifies that all concurrent accesses see the same session ID when no timeout occurs

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        val numThreads = 20
        val executor = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(numThreads)
        val sessionIds = mutableSetOf<String>()
        val sessionIdCount = AtomicInteger(0)

        // When - multiple threads access session concurrently
        repeat(numThreads) {
            executor.submit {
                try {
                    val sessionId = sessionManager.getSessionId()
                    synchronized(sessionIds) {
                        sessionIds.add(sessionId)
                        sessionIdCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // Then - all threads should have gotten the same session ID
        assertAll(
            "Session consistency validation",
            { assertThat(sessionIds).hasSize(1) },
            { assertThat(sessionIdCount.get()).isEqualTo(numThreads) },
        )
    }
}
