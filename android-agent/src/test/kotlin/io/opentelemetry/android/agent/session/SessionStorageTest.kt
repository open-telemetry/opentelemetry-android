/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.sdk.testing.time.TestClock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(Incubating::class)
internal class SessionStorageTest {
    private val clock = TestClock.create()
    private val config = SessionConfig(backgroundInactivityTimeout = 1.minutes, maxLifetime = 1.hours)
    private val timeoutHandler = SessionIdTimeoutHandler(config, clock)

    @Test
    fun `startup saves an empty session without reading stored state`() {
        val storage = mockk<SessionStorage>()
        val saved = mutableListOf<Session>()
        val previousId = "a".repeat(32)
        every { storage.get() } returns SessionImpl(previousId, clock.now())
        every { storage.save(capture(saved)) } returns Unit

        val manager = createManager(storage)
        assertThat(saved).containsExactly(invalidSession)

        val id = manager.getSessionId()
        assertThat(id).isNotEmpty().isNotEqualTo(previousId)
        assertThat(saved).hasSize(2)
        assertThat(saved.last().id).isEqualTo(id)
        assertThat(saved.last().startTimestamp).isEqualTo(clock.now())
        verify(exactly = 0) { storage.get() }
    }

    @Test
    fun `storage preserves inactivity and lifetime expiry and observer ordering`() {
        val storage = mockk<SessionStorage>(relaxed = true)
        val saved = mutableListOf<Session>()
        every { storage.save(capture(saved)) } returns Unit
        val observer = mockk<SessionObserver>(relaxed = true)
        val manager = createManager(storage)
        manager.addObserver(observer)

        val first = manager.getSessionId()
        timeoutHandler.onApplicationBackgrounded()
        clock.advance(59, TimeUnit.SECONDS)
        assertThat(manager.getSessionId()).isEqualTo(first)
        assertThat(saved).hasSize(2)

        // Reading the current session remains activity and extends the inactivity timeout.
        clock.advance(59, TimeUnit.SECONDS)
        assertThat(manager.getSessionId()).isEqualTo(first)
        clock.advance(1, TimeUnit.MINUTES)
        val second = manager.getSessionId()
        assertThat(second).isNotEqualTo(first)

        timeoutHandler.onApplicationForegrounded()
        assertThat(manager.getSessionId()).isEqualTo(second)
        clock.advance(1, TimeUnit.HOURS)
        val third = manager.getSessionId()
        assertThat(third).isNotEqualTo(second)
        assertThat(saved.map { it.id }).containsExactly("", first, second, third)
        verifyOrder {
            storage.save(saved[1])
            observer.onSessionEnded(invalidSession)
            observer.onSessionStarted(saved[1], invalidSession)
            storage.save(saved[2])
            observer.onSessionEnded(saved[1])
            observer.onSessionStarted(saved[2], saved[1])
            storage.save(saved[3])
            observer.onSessionEnded(saved[2])
            observer.onSessionStarted(saved[3], saved[2])
        }
        verify(exactly = 3) { observer.onSessionStarted(any(), any()) }
        verify(exactly = 3) { observer.onSessionEnded(any()) }
        verify(exactly = 0) { storage.get() }
    }

    @Test
    fun `handled storage failures do not interrupt sessions or observers`() {
        val backingStorage = mockk<SessionStorage>()
        every { backingStorage.save(any()) } throws IOException("unavailable")
        val cache = InMemorySessionStorage()
        val storage =
            object : SessionStorage {
                override fun get(): Session = cache.get()

                override fun save(newSession: Session) {
                    cache.save(newSession)
                    try {
                        backingStorage.save(newSession)
                    } catch (_: IOException) {
                        // This implementation chooses to retain the in-memory copy on failure.
                    }
                }
            }
        val observer = mockk<SessionObserver>(relaxed = true)
        val manager = createManager(storage)
        manager.addObserver(observer)
        val first = manager.getSessionId()
        clock.advance(1, TimeUnit.HOURS)
        val second = manager.getSessionId()

        assertThat(second).isNotEqualTo(first)
        assertThat(storage.get().id).isEqualTo(second)
        verify(exactly = 3) { backingStorage.save(any()) }
        verify(exactly = 2) { observer.onSessionStarted(any(), any()) }
        verify { observer.onSessionEnded(match { it.id == first }) }
    }

    @Test
    fun `unhandled storage failures propagate during initialization`() {
        val storage = mockk<SessionStorage>()
        val failure = IOException("unhandled")
        every { storage.save(any()) } throws failure

        assertThatThrownBy { createManager(storage) }.isSameAs(failure)
        verify(exactly = 1) { storage.save(invalidSession) }
        verify(exactly = 0) { storage.get() }
    }

    @Test
    fun `a new manager does not resume the session in reused storage`() {
        val storage = InMemorySessionStorage()
        val first = createManager(storage).getSessionId()
        val nextManager = createManager(storage)

        assertThat(storage.get()).isSameAs(invalidSession)
        assertThat(nextManager.getSessionId()).isNotEqualTo(first)
    }

    @Test
    fun `concurrent first access saves only one new session`() {
        val storage = mockk<SessionStorage>(relaxed = true)
        val manager = createManager(storage)
        val executor = Executors.newFixedThreadPool(4)
        try {
            val ids =
                executor
                    .invokeAll(List(20) { Callable { manager.getSessionId() } }, 5, TimeUnit.SECONDS)
                    .map { it.get() }
            assertThat(ids.toSet()).hasSize(1)
            verify(exactly = 1) { storage.save(invalidSession) }
            verify(exactly = 1) { storage.save(match { it.id == ids.first() }) }
            verify(exactly = 0) { storage.get() }
        } finally {
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()
        }
    }

    private fun createManager(storage: SessionStorage): SessionManager = SessionManager.create(timeoutHandler, config, clock, storage)
}
