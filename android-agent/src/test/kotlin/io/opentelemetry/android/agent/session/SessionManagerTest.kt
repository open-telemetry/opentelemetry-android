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
import java.util.Collections.synchronizedList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.hours

private const val SESSION_ID_LENGTH = 32
private const val MAX_SESSION_LIFETIME = 4L

/**
 * Verifies [SessionManager] functionality including session ID generation, timeouts, lifecycle
 * transitions, observer notifications, and thread-safety under concurrent access.
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

        // All threads should have gotten the same session ID
        assertAll(
            "Session consistency validation",
            { assertThat(sessionIds).hasSize(1) },
            { assertThat(sessionIdCount.get()).isEqualTo(numThreads) },
        )
    }

    @Test
    fun `getPreviousSessionId should return empty for initial session`() {
        // Verifies that initial session has no previous session ID

        // Given/When
        val sessionManager =
            SessionManager(
                TestClock.create(),
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        // Then - Initial session should have no previous session
        assertThat(sessionManager.getPreviousSessionId()).isEmpty()
    }

    @Test
    fun `getPreviousSessionId should return previous session after transition`() {
        // Verifies that previous session ID is tracked correctly after session transition

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        // When - Get initial session ID
        val firstSessionId = sessionManager.getSessionId()
        assertThat(sessionManager.getPreviousSessionId()).isEmpty()

        // When - Advance time to trigger session expiration
        clock.advance(5, TimeUnit.HOURS)

        // When - Get new session ID
        val secondSessionId = sessionManager.getSessionId()

        // Then - Verify new session is different and previous is tracked
        assertAll(
            "Session transition validation",
            { assertThat(secondSessionId).isNotEqualTo(firstSessionId) },
            { assertThat(sessionManager.getPreviousSessionId()).isEqualTo(firstSessionId) },
        )
    }

    @Test
    fun `getPreviousSessionId should track only most recent previous session`() {
        // Verifies that only the immediate previous session is tracked, not the entire history

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        // When - Get first session
        val firstSessionId = sessionManager.getSessionId()

        // When - Advance and get second session
        clock.advance(5, TimeUnit.HOURS)
        val secondSessionId = sessionManager.getSessionId()
        assertThat(sessionManager.getPreviousSessionId()).isEqualTo(firstSessionId)

        // When - Advance and get third session
        clock.advance(5, TimeUnit.HOURS)
        sessionManager.getSessionId()

        // Then - Previous should now be the second session, not the first
        assertAll(
            "Previous session tracking validation",
            { assertThat(sessionManager.getPreviousSessionId()).isEqualTo(secondSessionId) },
            { assertThat(sessionManager.getPreviousSessionId()).isNotEqualTo(firstSessionId) },
        )
    }

    @Test
    fun `getPreviousSessionId should be thread-safe during concurrent access`() {
        // Verifies that previous session ID is correctly tracked during concurrent session transitions

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        // Establish initial session
        val initialSessionId = sessionManager.getSessionId()

        // Advance time to trigger session expiration
        clock.advance(5, TimeUnit.HOURS)

        val numThreads = 10
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
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
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

    @Test
    fun `concurrent session observers should be notified correctly`() {
        // Verifies that multiple observers are all notified correctly during concurrent session transitions

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        val numObservers = 10
        val observers = mutableListOf<SessionObserver>()
        val startedSessions = synchronizedList(mutableListOf<String>())
        val endedSessions = synchronizedList(mutableListOf<String>())

        // Get initial session first
        val firstSessionId = sessionManager.getSessionId()

        // Add multiple observers after initial session is established
        repeat(numObservers) {
            val observer =
                mockk<SessionObserver> {
                    every { onSessionStarted(any(), any()) } answers {
                        startedSessions.add(firstArg<Session>().getId())
                    }
                    every { onSessionEnded(any()) } answers {
                        endedSessions.add(firstArg<Session>().getId())
                    }
                }
            observers.add(observer)
            sessionManager.addObserver(observer)
        }

        // Trigger session transition
        clock.advance(5, TimeUnit.HOURS)
        val secondSessionId = sessionManager.getSessionId()

        // All observers should have been notified of the transition
        assertAll(
            "Observer notification validation",
            { assertThat(startedSessions).hasSize(numObservers) },
            { assertThat(endedSessions).hasSize(numObservers) },
            { startedSessions.forEach { assertThat(it).isEqualTo(secondSessionId) } },
            { endedSessions.forEach { assertThat(it).isEqualTo(firstSessionId) } },
        )

        observers.forEach { observer ->
            verifyOrder {
                observer.onSessionEnded(any())
                observer.onSessionStarted(any(), any())
            }
        }
    }

    @Test
    fun `stress test with many concurrent threads accessing sessions`() {
        // Verifies that session manager handles high concurrency without errors or corruption

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        val numThreads = 50
        val iterationsPerThread = 100
        val executor: ExecutorService = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(numThreads)
        val allSessionIds = synchronizedList(mutableListOf<String>())
        val allPreviousIds = synchronizedList(mutableListOf<String>())
        val errors = synchronizedList(mutableListOf<Throwable>())

        repeat(numThreads) {
            executor.submit {
                try {
                    repeat(iterationsPerThread) {
                        val sessionId = sessionManager.getSessionId()
                        val previousId = sessionManager.getPreviousSessionId()
                        allSessionIds.add(sessionId)
                        allPreviousIds.add(previousId)
                    }
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // Verify no errors occurred and all operations completed
        assertAll(
            "Stress test validation",
            { assertThat(errors).isEmpty() },
            { assertThat(allSessionIds).hasSize(numThreads * iterationsPerThread) },
            { assertThat(allPreviousIds).hasSize(numThreads * iterationsPerThread) },
            { assertThat(allSessionIds.distinct()).isNotEmpty() },
            { assertThat(allSessionIds.all { it.isNotEmpty() }).isTrue() },
        )
    }

    @Test
    fun `concurrent session reads during multiple transitions should be consistent`() {
        // Verifies that concurrent reads within each session phase return consistent IDs

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        val numTransitions = 5
        val readsPerTransition = 20
        val sessionHistory = synchronizedList(mutableListOf<String>())

        repeat(numTransitions) { transitionIndex ->
            val executor: ExecutorService = Executors.newFixedThreadPool(readsPerTransition)
            val latch = CountDownLatch(readsPerTransition)
            val sessionIds = synchronizedList(mutableListOf<String>())

            // Launch concurrent reads
            repeat(readsPerTransition) {
                executor.submit {
                    try {
                        val sessionId = sessionManager.getSessionId()
                        sessionIds.add(sessionId)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
            executor.shutdown()

            // All reads in this phase should return the same session ID
            val uniqueIds = sessionIds.distinct()
            assertThat(uniqueIds).hasSize(1)

            sessionHistory.add(uniqueIds.first())

            // Advance time for next transition
            if (transitionIndex < numTransitions - 1) {
                clock.advance(5, TimeUnit.HOURS)
            }
        }

        // Verify that each transition produced a different session ID
        assertThat(sessionHistory.distinct()).hasSize(numTransitions)
    }

    @Test
    fun `mixed concurrent reads of current and previous session IDs should be consistent`() {
        // Verifies that concurrent reads of both current and previous IDs return consistent values

        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        // Establish initial session and transition
        val firstSessionId = sessionManager.getSessionId()
        clock.advance(5, TimeUnit.HOURS)
        val secondSessionId = sessionManager.getSessionId()

        val numThreads = 30
        val executor: ExecutorService = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(numThreads)
        val currentIds = synchronizedList(mutableListOf<String>())
        val previousIds = synchronizedList(mutableListOf<String>())

        // Half threads read current, half read previous
        repeat(numThreads) { threadIndex ->
            executor.submit {
                try {
                    if (threadIndex % 2 == 0) {
                        currentIds.add(sessionManager.getSessionId())
                    } else {
                        previousIds.add(sessionManager.getPreviousSessionId())
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // All current ID reads should return the same value
        // All previous ID reads should return the same value
        assertAll(
            "Mixed concurrent read validation",
            { assertThat(currentIds.distinct()).hasSize(1) },
            { assertThat(currentIds.first()).isEqualTo(secondSessionId) },
            { assertThat(previousIds.distinct()).hasSize(1) },
            { assertThat(previousIds.first()).isEqualTo(firstSessionId) },
        )
    }
}
