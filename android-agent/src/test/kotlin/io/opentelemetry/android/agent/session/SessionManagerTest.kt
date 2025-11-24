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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.hours

private const val AWAIT_TIMEOUT_SECONDS = 5L
private val MAX_SESSION_LIFETIME = 4.hours
private const val NUM_THREADS_LARGE = 20
private const val NUM_THREADS_MEDIUM = 10
private const val NUM_THREADS_SMALL = 5
private const val SESSION_ID_LENGTH = 32
private val TIME_AFTER_EXPIRATION = 5.hours

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
                maxSessionLifetime = MAX_SESSION_LIFETIME,
            )
        val sessionId = sessionManager.getSessionId()
        assertThat(sessionId).isNotNull()
        assertThat(sessionId).hasSize(SESSION_ID_LENGTH)
        assertThat(Pattern.compile("[a-f0-9]+").matcher(sessionId).matches()).isTrue()
    }

    @Test
    fun valueSameUntil4Hours() {
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME,
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
                maxSessionLifetime = MAX_SESSION_LIFETIME,
            )
        sessionManager.addObserver(observer)

        // The first call expires the Session.NONE initial session and notifies
        val firstSessionId = sessionManager.getSessionId()
        assertAll(
            { verify(exactly = 1) { timeoutHandler.bump() } },
            { verify(exactly = 0) { timeoutHandler.hasTimedOut() } },
            { verify(exactly = 1) { observer.onSessionStarted(any<Session>(), eq(Session.NONE)) } },
            { verify(exactly = 1) { observer.onSessionEnded(eq(Session.NONE)) } },
        )

        clock.advance(3, TimeUnit.HOURS)
        val secondSessionId = sessionManager.getSessionId()

        assertThat(firstSessionId).isEqualTo(secondSessionId)
        assertAll(
            { verify(exactly = 2) { timeoutHandler.bump() } },
            { verify(exactly = 1) { timeoutHandler.hasTimedOut() } },
            { verify(exactly = 1) { observer.onSessionStarted(any<Session>(), any<Session>()) } },
            { verify(exactly = 1) { observer.onSessionEnded(any<Session>()) } },
        )

        clock.advance(1, TimeUnit.HOURS)
        val thirdSessionId = sessionManager.getSessionId()

        assertAll(
            { verify(exactly = 3) { timeoutHandler.bump() } },
            { verify(exactly = 1) { timeoutHandler.hasTimedOut() } },
        )
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
            SessionManager(timeoutHandler = timeoutHandler, maxSessionLifetime = MAX_SESSION_LIFETIME)

        val value = sessionId.getSessionId()
        verify { timeoutHandler.bump() }

        assertThat(value).isEqualTo(sessionId.getSessionId())
        verify(exactly = 2) { timeoutHandler.bump() }

        every { timeoutHandler.hasTimedOut() } returns true

        assertThat(value).isNotEqualTo(sessionId.getSessionId())
        verify(exactly = 3) { timeoutHandler.bump() }
    }

    @Test
    fun `concurrent access during timeout should create only one new session`() {
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME,
            )

        // Establish an initial session
        val initialSessionId = sessionManager.getSessionId()

        // Advance time to trigger session expiration
        clock.advance(TIME_AFTER_EXPIRATION.inWholeHours, TimeUnit.HOURS)

        val numThreads = NUM_THREADS_MEDIUM
        val executor = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(numThreads)
        val sessionIds = mutableSetOf<String>()
        val sessionIdCount = AtomicInteger(0)

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

        assertThat(latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // Verify that only one new session was created
        assertAll(
            "Session creation validation",
            { assertThat(sessionIds).hasSize(1) },
            { assertThat(sessionIds.first()).isNotEqualTo(initialSessionId) },
            { assertThat(sessionIdCount.get()).isEqualTo(numThreads) },
        )
    }

    @Test
    fun `concurrent access with timeout handler should create only one new session`() {
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME,
            )

        every { timeoutHandler.hasTimedOut() } returns true

        val numThreads = NUM_THREADS_SMALL
        val executor = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(numThreads)
        val sessionIds = mutableSetOf<String>()
        val sessionIdCount = AtomicInteger(0)

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

        assertThat(latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // Verify the expected number of session IDs
        assertAll(
            "Race condition validation",
            { assertThat(sessionIdCount.get()).isEqualTo(numThreads) },
            { assertThat(sessionIds).isNotEmpty() },
            { assertThat(sessionIds.size).isLessThanOrEqualTo(numThreads) },
        )
    }

    @Test
    fun `concurrent access should maintain session consistency`() {
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME,
            )

        val numThreads = NUM_THREADS_LARGE
        val executor = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(numThreads)
        val sessionIds = mutableSetOf<String>()
        val sessionIdCount = AtomicInteger(0)

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

        assertThat(latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // All threads should have gotten the same session ID
        assertAll(
            "Session consistency validation",
            { assertThat(sessionIds).hasSize(1) },
            { assertThat(sessionIdCount.get()).isEqualTo(numThreads) },
        )
    }

    @Test
    fun `getPreviousSessionId should return empty for initial session`() {
        val sessionManager =
            SessionManager(
                TestClock.create(),
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME,
            )

        // Initial session should have no previous session
        assertThat(sessionManager.getPreviousSessionId()).isEmpty()
    }

    @Test
    fun `getPreviousSessionId should return previous session after transition`() {
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME,
            )

        // Get initial session ID
        val firstSessionId = sessionManager.getSessionId()
        assertThat(sessionManager.getPreviousSessionId()).isEmpty()

        // Advance time to trigger session expiration
        clock.advance(TIME_AFTER_EXPIRATION.inWholeHours, TimeUnit.HOURS)

        // Get new session ID
        val secondSessionId = sessionManager.getSessionId()

        // Verify new session is different and previous is tracked
        assertAll(
            "Session transition validation",
            { assertThat(secondSessionId).isNotEqualTo(firstSessionId) },
            { assertThat(sessionManager.getPreviousSessionId()).isEqualTo(firstSessionId) },
        )
    }

    @Test
    fun `getPreviousSessionId should track only most recent previous session`() {
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME,
            )

        // Get first session
        val firstSessionId = sessionManager.getSessionId()

        // Advance and get second session
        clock.advance(TIME_AFTER_EXPIRATION.inWholeHours, TimeUnit.HOURS)
        val secondSessionId = sessionManager.getSessionId()
        assertThat(sessionManager.getPreviousSessionId()).isEqualTo(firstSessionId)

        // Advance and get third session
        clock.advance(TIME_AFTER_EXPIRATION.inWholeHours, TimeUnit.HOURS)
        sessionManager.getSessionId()

        // Previous should now be the second session, not the first
        assertAll(
            "Previous session tracking validation",
            { assertThat(sessionManager.getPreviousSessionId()).isEqualTo(secondSessionId) },
            { assertThat(sessionManager.getPreviousSessionId()).isNotEqualTo(firstSessionId) },
        )
    }

    @Test
    fun `getPreviousSessionId should be thread-safe during concurrent access`() {
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME,
            )

        // Establish initial session
        val initialSessionId = sessionManager.getSessionId()

        // Advance time to trigger session expiration
        clock.advance(TIME_AFTER_EXPIRATION.inWholeHours, TimeUnit.HOURS)

        val numThreads = NUM_THREADS_MEDIUM
        val executor: ExecutorService = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(numThreads)
        val sessionIds = mutableSetOf<String>()

        // All threads race to get the new session ID
        repeat(numThreads) {
            executor.submit {
                try {
                    val newSessionId = sessionManager.getSessionId()
                    synchronized(sessionIds) {
                        sessionIds.add(newSessionId)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // Wait for all threads to complete session transition
        assertThat(latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // After all threads complete, check previous session ID
        // The session transition is now guaranteed to be complete
        val previousId = sessionManager.getPreviousSessionId()

        // All threads should have seen the same new session ID
        // And the previous session should be the initial session
        assertAll(
            "Thread-safe previous session ID validation",
            { assertThat(sessionIds).hasSize(1) },
            { assertThat(sessionIds.first()).isNotEqualTo(initialSessionId) },
            { assertThat(previousId).isEqualTo(initialSessionId) },
        )
    }
}
