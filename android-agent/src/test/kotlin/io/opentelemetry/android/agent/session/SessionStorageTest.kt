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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(Incubating::class)
internal class SessionStorageTest {
    private val clock = TestClock.create()
    private val config = SessionConfig(backgroundInactivityTimeout = 1.minutes, maxLifetime = 1.hours)
    private val timeoutHandler = SessionIdTimeoutHandler(config, clock)

    @Test
    fun `startup leaves storage untouched and first access saves a new session`() {
        val storage = mockk<SessionStorage>()
        val saved = mutableListOf<Session>()
        val previousId = "a".repeat(32)
        every { storage.get() } returns SessionImpl(previousId, clock.now())
        every { storage.save(capture(saved)) } returns Unit

        val manager = createManager(storage)
        assertThat(saved).isEmpty()

        val id = manager.getSessionId()
        assertThat(id).isNotEmpty().isNotEqualTo(previousId)
        assertThat(saved).hasSize(1)
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
        assertThat(saved).hasSize(1)

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
        assertThat(saved.map { it.id }).containsExactly(first, second, third)
        verifyOrder {
            storage.save(saved[0])
            observer.onSessionEnded(invalidSession)
            observer.onSessionStarted(saved[0], invalidSession)
            storage.save(saved[1])
            observer.onSessionEnded(saved[0])
            observer.onSessionStarted(saved[1], saved[0])
            storage.save(saved[2])
            observer.onSessionEnded(saved[1])
            observer.onSessionStarted(saved[2], saved[1])
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
        verify(exactly = 2) { backingStorage.save(any()) }
        verify(exactly = 2) { observer.onSessionStarted(any(), any()) }
        verify { observer.onSessionEnded(match { it.id == first }) }
    }

    @Test
    fun `unhandled storage and observer failures do not prevent later transitions`() {
        for (failDuringSave in listOf(true, false)) {
            val storage = mockk<SessionStorage>(relaxed = true)
            val observer = mockk<SessionObserver>(relaxed = true)
            val failure = IOException("unhandled")
            if (failDuringSave) {
                every { storage.save(any()) } throws failure
            } else {
                every { observer.onSessionStarted(any(), any()) } throws failure
            }
            val manager = createManager(storage)
            manager.addObserver(observer)
            assertThatThrownBy { manager.getSessionId() }.isSameAs(failure)
            val first = manager.getSessionId()
            every { storage.save(any()) } returns Unit
            every { observer.onSessionStarted(any(), any()) } returns Unit
            clock.advance(1, TimeUnit.HOURS)
            val second = manager.getSessionId()
            assertThat(second).isNotEqualTo(first)
            verify(exactly = 2) { storage.save(any()) }
            verify { observer.onSessionStarted(match { it.id == second }, match { it.id == first }) }
            verify(exactly = 0) { storage.get() }
        }
    }

    @Test
    fun `a new manager does not resume the session in reused storage`() {
        val storage = InMemorySessionStorage()
        val first = createManager(storage).getSessionId()
        val nextManager = createManager(storage)

        assertThat(storage.get().id).isEqualTo(first)
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
            verify(exactly = 0) { storage.save(invalidSession) }
            verify(exactly = 1) { storage.save(match { it.id == ids.first() }) }
            verify(exactly = 0) { storage.get() }
        } finally {
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()
        }
    }

    @Test
    fun `active session reads do not wait for another reader`() {
        val handler = mockk<SessionIdTimeoutHandler>(relaxed = true)
        val manager = SessionManager(clock, timeoutHandler = handler, maxSessionLifetime = config.maxLifetime)
        val id = manager.getSessionId()
        val paused = CountDownLatch(1)
        val resume = CountDownLatch(1)
        val pauseNext = AtomicBoolean(true)
        every { handler.bump() } answers {
            if (pauseNext.compareAndSet(true, false)) {
                paused.countDown()
                check(resume.await(5, TimeUnit.SECONDS))
            }
        }
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit(Callable { manager.getSessionId() })
            assertThat(paused.await(5, TimeUnit.SECONDS)).isTrue()
            val second = executor.submit(Callable { manager.getSessionId() })
            assertThat(second.get(1, TimeUnit.SECONDS)).isEqualTo(id)
            resume.countDown()
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(id)
        } finally {
            resume.countDown()
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()
        }
    }

    @Test
    fun `session reads do not wait for storage or observers`() {
        for (pauseDuringSave in listOf(true, false)) verifyNonblockingTransition(pauseDuringSave, advanceWhilePaused = null)
    }

    @Test
    fun `later expiry cannot overtake storage or observer notifications`() {
        for (pauseDuringSave in listOf(true, false)) {
            for (expiry in listOf(TimeUnit.MINUTES, TimeUnit.HOURS)) verifyNonblockingTransition(pauseDuringSave, expiry)
        }
    }

    private fun verifyNonblockingTransition(
        pauseDuringSave: Boolean,
        advanceWhilePaused: TimeUnit?,
    ) {
        val paused = CountDownLatch(1)
        val resume = CountDownLatch(1)
        val events = CopyOnWriteArrayList<String>()
        var shouldPause = false

        fun pauseOnce() {
            if (shouldPause) {
                shouldPause = false
                paused.countDown()
                check(resume.await(5, TimeUnit.SECONDS))
            }
        }
        val storage =
            object : SessionStorage {
                override fun get(): Session = error("The manager must not read storage")

                override fun save(newSession: Session) {
                    if (pauseDuringSave) pauseOnce()
                    events.add("save:${newSession.id}")
                }
            }
        val manager = createManager(storage)
        manager.addObserver(
            object : SessionObserver {
                override fun onSessionEnded(session: Session) {
                    if (!pauseDuringSave) pauseOnce()
                    events.add("end:${session.id}")
                }

                override fun onSessionStarted(
                    newSession: Session,
                    previousSession: Session,
                ) {
                    events.add("start:${newSession.id}")
                }
            },
        )
        val initial = manager.getSessionId()
        events.clear()
        timeoutHandler.onApplicationBackgrounded()
        clock.advance(1, TimeUnit.MINUTES)
        shouldPause = true
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit(Callable { manager.getSessionId() })
            assertThat(paused.await(5, TimeUnit.SECONDS)).isTrue()
            advanceWhilePaused?.let { clock.advance(1, it) }
            val second = executor.submit(Callable { manager.getSessionId() })
            val secondId = second.get(1, TimeUnit.SECONDS)
            assertThat(secondId).isNotEqualTo(initial)
            resume.countDown()
            val firstId = first.get(5, TimeUnit.SECONDS)
            assertThat(firstId).isEqualTo(secondId)
            val expected = mutableListOf("save:$firstId", "end:$initial", "start:$firstId")
            assertThat(events).containsExactlyElementsOf(expected)
            if (advanceWhilePaused != null) {
                val nextId = manager.getSessionId()
                assertThat(nextId).isNotEqualTo(firstId)
                expected.addAll(listOf("save:$nextId", "end:$firstId", "start:$nextId"))
            }
            assertThat(events).containsExactlyElementsOf(expected)
        } finally {
            resume.countDown()
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()
        }
    }

    @Test
    fun `storage and observers can read the new session during an inactivity transition`() {
        val observedIds = mutableListOf<String>()
        val executor = Executors.newSingleThreadExecutor()
        lateinit var manager: SessionManager

        fun readSession() {
            val id = manager.getSessionId()
            assertThat(executor.submit(Callable { manager.getSessionId() }).get(1, TimeUnit.SECONDS)).isEqualTo(id)
            observedIds.add(id)
        }
        val storage =
            object : SessionStorage {
                override fun get(): Session = error("The manager must not read storage")

                override fun save(newSession: Session) {
                    if (newSession.id.isNotEmpty()) readSession()
                }
            }
        manager = createManager(storage)
        manager.addObserver(
            object : SessionObserver {
                override fun onSessionEnded(session: Session) {
                    readSession()
                }

                override fun onSessionStarted(
                    newSession: Session,
                    previousSession: Session,
                ) {
                    readSession()
                }
            },
        )
        try {
            val first = manager.getSessionId()
            timeoutHandler.onApplicationBackgrounded()
            clock.advance(1, TimeUnit.MINUTES)
            val second = manager.getSessionId()

            assertThat(second).isNotEqualTo(first)
            assertThat(observedIds).containsExactly(first, first, first, second, second, second)
        } finally {
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()
        }
    }

    private fun createManager(storage: SessionStorage): SessionManager = SessionManager.create(timeoutHandler, config, clock, storage)
}
