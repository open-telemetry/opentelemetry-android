/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.sdk.testing.time.TestClock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(Incubating::class)
class SessionResetTest {
    private val clock = TestClock.create()
    private val timeout = SessionIdTimeoutHandler(clock, 15.minutes)
    private val storage = InMemorySessionStorage()
    private val manager = SessionManager(clock, storage, timeout, maxSessionLifetime = 4.hours)
    private val events = mutableListOf<String>()
    private val observer =
        object : SessionObserver {
            override fun onSessionEnded(session: Session) {
                events.add("end:${session.id}")
            }

            override fun onSessionStarted(
                newSession: Session,
                previousSession: Session,
            ) {
                assertThat(storage.get().id).isEqualTo(newSession.id)
                events.add("start:${newSession.id}:${previousSession.id}")
            }
        }

    @Test
    fun `reset creates exactly one stored linked transition without waiting for expiry`() {
        manager.addObserver(observer)
        val first = manager.getSessionId()
        events.clear()
        assertThat(manager.resetSession()).isTrue()
        val second = manager.getSessionId()
        assertThat(second).isNotEqualTo(first)
        assertThat(events).containsExactly("end:$first", "start:$second:$first")
        assertThat(manager.getSessionId()).isEqualTo(second)
        assertThat(storage.get().id).isEqualTo(second)
    }

    @Test
    fun `reset before first lookup creates only one session`() {
        manager.addObserver(observer)
        assertThat(manager.resetSession()).isTrue()
        val first = manager.getSessionId()
        assertThat(events).containsExactly("end:", "start:$first:")
        assertThat(storage.get().id).isEqualTo(first)
    }

    @Test
    fun `reset after expiry does not create an intermediate session and resets both timeouts`() {
        manager.addObserver(observer)
        val first = manager.getSessionId()
        timeout.onApplicationBackgrounded()
        clock.advance(4, HOURS)
        events.clear()
        assertThat(manager.resetSession()).isTrue()
        val second = manager.getSessionId()
        assertThat(events).containsExactly("end:$first", "start:$second:$first")
        assertThat(timeout.hasTimedOut()).isFalse()
        assertThat(storage.get().startTimestamp).isEqualTo(clock.now())
    }

    @Test
    fun `reset from a callback returns busy without recursion or a partial notification sequence`() {
        val attempts = mutableListOf<Boolean>()
        manager.addObserver(
            object : SessionObserver {
                override fun onSessionEnded(session: Session) {
                    attempts.add(manager.resetSession())
                }

                override fun onSessionStarted(
                    newSession: Session,
                    previousSession: Session,
                ) {
                    assertThat(manager.getSessionId()).isEqualTo(newSession.id)
                    attempts.add(manager.resetSession())
                }
            },
        )
        manager.addObserver(observer)
        manager.getSessionId()
        assertThat(attempts).containsExactly(false, false)
        attempts.clear()
        assertThat(manager.resetSession()).isTrue()
        assertThat(attempts).containsExactly(false, false)
        assertThat(events).hasSize(4)
    }

    @Test
    fun `concurrent reset never waits for a slow callback and can be retried afterward`() {
        val first = manager.getSessionId()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        manager.addObserver(
            object : SessionObserver {
                override fun onSessionEnded(session: Session) {
                    entered.countDown()
                    check(release.await(5, SECONDS))
                }

                override fun onSessionStarted(
                    newSession: Session,
                    previousSession: Session,
                ) {}
            },
        )
        manager.addObserver(observer)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val firstReset = executor.submit<Boolean> { manager.resetSession() }
            assertThat(entered.await(5, SECONDS)).isTrue()
            assertThat(executor.submit<Boolean> { manager.resetSession() }.get(1, SECONDS)).isFalse()
            val second = manager.getSessionId()
            assertThat(second).isNotEqualTo(first)
            release.countDown()
            assertThat(firstReset.get(5, SECONDS)).isTrue()
            assertThat(manager.resetSession()).isTrue()
            val third = manager.getSessionId()
            assertThat(events).containsExactly("end:$first", "start:$second:$first", "end:$second", "start:$third:$second")
        } finally {
            release.countDown()
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue()
        }
    }
}
