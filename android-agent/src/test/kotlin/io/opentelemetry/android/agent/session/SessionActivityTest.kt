/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.mockk.every
import io.mockk.spyk
import io.opentelemetry.android.SessionIdRatioBasedSampler
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.testing.time.TestClock
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SessionActivityTest {
    private val clock = TestClock.create()
    private val timeout = SessionIdTimeoutHandler(clock, 15.minutes)
    private val manager = SessionManager(clock, timeoutHandler = timeout, maxSessionLifetime = 4.hours)

    @Test
    fun `background lookups create then rotate without extending inactivity`() {
        timeout.onApplicationBackgrounded()
        val first = manager.getSessionId()
        assertThat(first).hasSize(32)
        repeat(2) {
            clock.advance(7, MINUTES)
            assertThat(manager.getSessionId()).isEqualTo(first)
        }
        clock.advance(1, MINUTES)
        val second = manager.getSessionId()
        assertThat(second).isNotEqualTo(first)
        assertThat(manager.getSessionId()).isEqualTo(second)
        clock.advance(15, MINUTES)
        assertThat(manager.getSessionId()).isNotEqualTo(second)
    }

    @Test
    fun `dropped sampling attempts cannot keep a background session alive`() {
        val sampler = SessionIdRatioBasedSampler(0.0, manager)
        val first = manager.getSessionId()
        timeout.onApplicationBackgrounded()
        repeat(3) {
            clock.advance(5, MINUTES)
            assertThat(
                sampler.shouldSample(Context.root(), first, "background", SpanKind.INTERNAL, Attributes.empty(), emptyList()).decision,
            ).isEqualTo(SamplingDecision.DROP)
        }
        assertThat(manager.getSessionId()).isNotEqualTo(first)
    }

    @Test
    fun `user interaction extends a valid session but rotates an expired one`() {
        val recorder: SessionUserInteractionRecorder = manager
        recorder.recordUserInteraction()
        val first = manager.getSessionId()
        timeout.onApplicationBackgrounded()
        clock.advance(14, MINUTES)
        recorder.recordUserInteraction()
        clock.advance(14, MINUTES)
        assertThat(manager.getSessionId()).isEqualTo(first)
        clock.advance(1, MINUTES)
        recorder.recordUserInteraction()
        val second = manager.getSessionId()
        assertThat(second).isNotEqualTo(first)
        assertThat(timeout.hasTimedOut()).isFalse()
        timeout.onApplicationForegrounded()
        clock.advance(240, MINUTES)
        manager.recordUserInteraction()
        assertThat(manager.getSessionId()).isNotEqualTo(second)
    }

    @Test
    fun `background timeout starts when leaving a quiet foreground`() {
        val first = manager.getSessionId()
        clock.advance(60, MINUTES)
        timeout.onApplicationBackgrounded()
        clock.advance(14, MINUTES)
        assertThat(manager.getSessionId()).isEqualTo(first)
        timeout.onApplicationBackgrounded()
        clock.advance(1, MINUTES)
        assertThat(manager.getSessionId()).isNotEqualTo(first)
    }

    @Test
    fun `short background visit does not expire later in the foreground`() {
        val first = manager.getSessionId()
        timeout.onApplicationBackgrounded()
        clock.advance(14, MINUTES)
        timeout.onApplicationForegrounded()
        clock.advance(60, MINUTES)
        assertThat(manager.getSessionId()).isEqualTo(first)
        timeout.onApplicationBackgrounded()
        clock.advance(14, MINUTES)
        assertThat(manager.getSessionId()).isEqualTo(first)
        clock.advance(1, MINUTES)
        assertThat(manager.getSessionId()).isNotEqualTo(first)
    }

    @Test
    fun `foreground return preserves expiry until lookup even across another background visit`() {
        val first = manager.getSessionId()
        timeout.onApplicationBackgrounded()
        clock.advance(15, MINUTES)
        timeout.onApplicationForegrounded()
        timeout.onApplicationForegrounded()
        timeout.onApplicationBackgrounded()
        val second = manager.getSessionId()
        assertThat(second).isNotEqualTo(first)
        assertThat(manager.getSessionId()).isEqualTo(second)
    }

    @Test
    fun `simultaneous lookups publish one initial session and one expiry transition`() {
        val events = CopyOnWriteArrayList<String>()
        manager.addObserver(
            object : SessionObserver {
                override fun onSessionEnded(session: Session) {
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
        timeout.onApplicationBackgrounded()
        val executor = Executors.newFixedThreadPool(8)
        try {
            var previous = ""
            repeat(2) {
                val ready = CountDownLatch(8)
                val start = CountDownLatch(1)
                val reads =
                    (1..8).map {
                        executor.submit<String> {
                            ready.countDown()
                            check(start.await(5, SECONDS))
                            manager.getSessionId()
                        }
                    }
                assertThat(ready.await(5, SECONDS)).isTrue()
                start.countDown()
                val ids = reads.map { it.get(5, SECONDS) }.toSet()
                assertThat(ids).hasSize(1).doesNotContain(previous)
                val current = ids.single()
                assertThat(events).containsExactly("end:$previous", "start:$current")
                previous = current
                events.clear()
                clock.advance(15, MINUTES)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `concurrent readers rotate once and callbacks can read without blocking`() {
        val first = manager.getSessionId()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val events = mutableListOf<String>()
        manager.addObserver(
            object : SessionObserver {
                override fun onSessionEnded(session: Session) {
                    events.add("end:${session.id}")
                    assertThat(manager.getSessionId()).isNotEqualTo(first)
                    entered.countDown()
                    check(release.await(5, SECONDS))
                }

                override fun onSessionStarted(
                    newSession: Session,
                    previousSession: Session,
                ) {
                    events.add("start:${newSession.id}")
                }
            },
        )
        timeout.onApplicationBackgrounded()
        clock.advance(15, MINUTES)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val rotation = executor.submit<String> { manager.getSessionId() }
            assertThat(entered.await(5, SECONDS)).isTrue()
            val readers = (1..7).map { executor.submit<String> { manager.getSessionId() } }
            val ids = readers.map { it.get(5, SECONDS) }
            assertThat(ids.toSet()).hasSize(1).doesNotContain(first)
            clock.advance(15, MINUTES)
            // Neither reads nor user interaction may interrupt the notification sequence or revive expiry.
            val interaction = executor.submit { manager.recordUserInteraction() }
            interaction.get(5, SECONDS)
            assertThat(manager.getSessionId()).isEqualTo(ids.first())
            assertThat(timeout.hasTimedOut()).isTrue()
            release.countDown()
            assertThat(rotation.get(5, SECONDS)).isEqualTo(ids.first())
            val next = manager.getSessionId()
            assertThat(next).isNotEqualTo(ids.first())
            assertThat(events).containsExactly("end:$first", "start:${ids.first()}", "end:${ids.first()}", "start:$next")
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `resetting the timeout cannot expose the previous expired session`() = assertConcurrentRotation(pauseAfterTimeoutReset = true)

    @Test
    fun `lookup rechecks its session snapshot after a concurrent rotation`() = assertConcurrentRotation(pauseAfterTimeoutReset = false)

    private fun assertConcurrentRotation(pauseAfterTimeoutReset: Boolean) {
        val paused = CountDownLatch(1)
        val resume = CountDownLatch(1)
        val readerThread = AtomicReference<Thread?>()
        val testClock =
            object : Clock by clock {
                override fun now(): Long {
                    if (!pauseAfterTimeoutReset && readerThread.compareAndSet(Thread.currentThread(), null)) {
                        paused.countDown()
                        check(resume.await(5, SECONDS))
                    }
                    return clock.now()
                }
            }
        val handler = spyk(SessionIdTimeoutHandler(testClock, 15.minutes))
        val manager = SessionManager(testClock, timeoutHandler = handler, maxSessionLifetime = 4.hours)
        val first = manager.getSessionId()
        handler.onApplicationBackgrounded()
        clock.advance(15, MINUTES)
        if (pauseAfterTimeoutReset) {
            every { handler.bump() } answers {
                callOriginal()
                paused.countDown()
                check(resume.await(5, SECONDS))
            }
        }
        val executor = Executors.newFixedThreadPool(2)
        try {
            val pausedLookup =
                executor.submit<String> {
                    readerThread.set(Thread.currentThread())
                    manager.getSessionId()
                }
            assertThat(paused.await(5, SECONDS)).isTrue()
            val read = executor.submit<String> { manager.getSessionId() }
            val id = read.get(5, SECONDS)
            assertThat(id).isNotEqualTo(first)
            resume.countDown()
            assertThat(pausedLookup.get(5, SECONDS)).isEqualTo(id)
        } finally {
            resume.countDown()
            executor.shutdownNow()
        }
    }
}
