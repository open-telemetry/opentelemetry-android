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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.hours

private const val SESSION_AWAIT_SECONDS: Long = 5
private const val SESSION_ID_LENGTH = 32
private const val MAX_SESSION_LIFETIME: Long = 4

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
    fun `generated session IDs are valid 32-character hex strings`() {
        // Given/When
        val sessionManager =
            SessionManager(
                TestClock.create(),
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )
        val sessionId = sessionManager.getSessionId()
        val sessionIdPattern = "[a-f0-9]+"

        // Then
        assertThat(sessionId).isNotNull()
        assertThat(sessionId).hasSize(SESSION_ID_LENGTH)
        assertThat(Pattern.compile(sessionIdPattern).matcher(sessionId).matches()).isTrue()
    }

    @Test
    fun valueSameUntil4Hours() {
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
        assertThat(newSessionId).isNotNull()
        assertThat(value).isNotEqualTo(newSessionId)
    }

    @Test
    fun shouldCallSessionIdChangeListener() {
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
        verify(exactly = 1) { timeoutHandler.bump() }
        verify(exactly = 0) { timeoutHandler.hasTimedOut() }
        verify(exactly = 1) { observer.onSessionStarted(any<Session>(), eq(Session.NONE)) }
        verify(exactly = 1) { observer.onSessionEnded(eq(Session.NONE)) }

        // When
        clock.advance(3, TimeUnit.HOURS)
        val secondSessionId = sessionManager.getSessionId()

        // Then
        assertThat(firstSessionId).isEqualTo(secondSessionId)
        verify(exactly = 2) { timeoutHandler.bump() }
        verify(exactly = 1) { timeoutHandler.hasTimedOut() }
        verify(exactly = 1) { observer.onSessionStarted(any<Session>(), any<Session>()) }
        verify(exactly = 1) { observer.onSessionEnded(any<Session>()) }

        // When
        clock.advance(1, TimeUnit.HOURS)
        val thirdSessionId = sessionManager.getSessionId()

        // Then
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
        // Given
        val sessionId =
            SessionManager(
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours
            )

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
        val firstLatch = CountDownLatch(numThreads)
        val lastLatch = CountDownLatch(numThreads)
        val sessionIds = mutableSetOf<String>()
        val sessionIdCount = AtomicInteger(0)

        // When - multiple threads access session concurrently after timeout
        val params = AddSessionIdsParameters(
            numThreads,
            executor,
            firstLatch,
            lastLatch,
            sessionManager,
            sessionIds,
            sessionIdCount
        )
        addSessionIdsAcrossThreads(params)

        val isCountZero = lastLatch.await(SESSION_AWAIT_SECONDS, TimeUnit.SECONDS)
        assertThat(isCountZero).isTrue()
        executor.shutdown()

        // Then - verify that only one new session was created
        assertThat(sessionIds).hasSize(1)
        assertThat(sessionIds.first()).isNotEqualTo(initialSessionId)
        assertThat(sessionIdCount.get()).isEqualTo(numThreads)
    }

    @Test
    fun `concurrent access with timeout handler should create only one new session`() {
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
        val firstLatch = CountDownLatch(numThreads)
        val lastLatch = CountDownLatch(numThreads)
        val sessionIds = mutableSetOf<String>()
        val sessionIdCount = AtomicInteger(0)

        // When - multiple threads access session with timeout handler indicating timeout
        val params = AddSessionIdsParameters(
            numThreads,
            executor,
            firstLatch,
            lastLatch,
            sessionManager,
            sessionIds,
            sessionIdCount
        )
        addSessionIdsAcrossThreads(params)

        val isCountZero = lastLatch.await(SESSION_AWAIT_SECONDS, TimeUnit.SECONDS)
        assertThat(isCountZero).isTrue()
        executor.shutdown()

        // Then - verify the expected number of session IDs
        assertThat(sessionIdCount.get()).isEqualTo(numThreads)
    }

    @Test
    fun `concurrent access should accesses see the same session ID when no timeout occurs`() {
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
        val firstLatch = CountDownLatch(numThreads)
        val lastLatch = CountDownLatch(numThreads)
        val sessionIds = mutableSetOf<String>()
        val sessionIdCount = AtomicInteger(0)

        // When - multiple threads access session concurrently
        val params = AddSessionIdsParameters(
            numThreads,
            executor,
            firstLatch,
            lastLatch,
            sessionManager,
            sessionIds,
            sessionIdCount
        )
        addSessionIdsAcrossThreads(params)

        val isCountZero = lastLatch.await(SESSION_AWAIT_SECONDS, TimeUnit.SECONDS)
        assertThat(isCountZero).isTrue()
        executor.shutdown()

        // Then - all threads should have gotten the same session ID
        assertThat(sessionIds).hasSize(1)
        assertThat(sessionIdCount.get()).isEqualTo(numThreads)
    }

    @Test
    fun `session expiration check uses correct session instance`() {
        // Given
        val clock = TestClock.create()
        val sessionManager =
            SessionManager(
                clock,
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = MAX_SESSION_LIFETIME.hours,
            )

        // When - get initial session
        val firstSessionId = sessionManager.getSessionId()

        // Then - verify session is stable with ids match
        clock.advance(2, TimeUnit.HOURS)
        assertThat(sessionManager.getSessionId()).isEqualTo(firstSessionId)

        clock.advance(1, TimeUnit.HOURS)
        assertThat(sessionManager.getSessionId()).isEqualTo(firstSessionId)

        // When - advance time to exactly the expiration boundary
        clock.advance(59, TimeUnit.MINUTES)
        clock.advance(59, TimeUnit.SECONDS)

        // Then - session should still be the same (3h 59m 59s < 4h)
        assertThat(sessionManager.getSessionId()).isEqualTo(firstSessionId)

        // When - advance 1 second to exceed maxLifetime
        clock.advance(1, TimeUnit.SECONDS)
        val secondSessionId = sessionManager.getSessionId()

        // Then - new session should have been created
        assertThat(secondSessionId).isNotEqualTo(firstSessionId)
        assertThat(secondSessionId).isNotNull()
        assertThat(secondSessionId).hasSize(SESSION_ID_LENGTH)

        // When - verify ids match
        clock.advance(3, TimeUnit.HOURS)
        assertThat(sessionManager.getSessionId()).isEqualTo(secondSessionId)

        // When - expire second session
        clock.advance(1, TimeUnit.HOURS)
        clock.advance(1, TimeUnit.SECONDS)
        val thirdSessionId = sessionManager.getSessionId()

        // Then - third session should be different from both previous sessions
        assertThat(thirdSessionId).isNotEqualTo(firstSessionId)
        assertThat(thirdSessionId).isNotEqualTo(secondSessionId)
    }

    private fun addSessionIdsAcrossThreads(
        params: AddSessionIdsParameters
    ) {
        repeat(params.numThreads) {
            params.executor.submit {
                try {
                    params.firstLatch.countDown()
                    params.firstLatch.await(SESSION_AWAIT_SECONDS, TimeUnit.SECONDS)

                    val sessionId = params.sessionManager.getSessionId()
                    synchronized(params.sessionIds) {
                        params.sessionIds.add(sessionId)
                        params.sessionIdCount.incrementAndGet()
                    }
                } finally {
                    params.lastLatch.countDown()
                }
            }
        }
    }

    private data class AddSessionIdsParameters(
        val numThreads: Int,
        val executor: ExecutorService,
        val firstLatch: CountDownLatch,
        val lastLatch: CountDownLatch,
        val sessionManager: SessionManager,
        val sessionIds: MutableSet<String>,
        val sessionIdCount: AtomicInteger,
    )
}
