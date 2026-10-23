/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.RumBuilder
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.sdk.testing.time.TestClock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(Incubating::class)
class SessionExtensionTest {
    private val clock = TestClock.create()
    private val storage = InMemorySessionStorage()
    private val timeout = SessionIdTimeoutHandler(clock, 15.minutes)
    private val manager = SessionManager(clock, storage, timeout, maxSessionLifetime = 4.hours)
    private val extension: SessionExtension = SessionExtensionImpl(manager)

    @Test
    fun `forwards the result without retrying a busy transition`() {
        for (accepted in listOf(true, false)) {
            val manager = mockk<SessionManager>()
            every { manager.resetSession() } returns accepted
            assertThat(SessionExtensionImpl(manager).startNewSession()).isEqualTo(accepted)
            verify(exactly = 1) { manager.resetSession() }
        }
    }

    @Test
    fun `does not retry or hide a partially delivered transition`() {
        val manager = mockk<SessionManager>()
        val failure = IllegalStateException("observer failed")
        every { manager.resetSession() } throws failure
        assertThatThrownBy { SessionExtensionImpl(manager).startNewSession() }.isSameAs(failure)
        verify(exactly = 1) { manager.resetSession() }
    }

    @Test
    fun `first manual start creates a session without a telemetry lookup`() {
        assertThat(extension.type).isEqualTo(SessionExtension::class.java)
        assertThat(extension.startNewSession()).isTrue()
        val stored = storage.get()
        assertThat(stored.id).hasSize(32)
        assertThat(manager.getSessionId()).isEqualTo(stored.id)
        assertThat(storage.get()).isSameAs(stored)
    }

    @Test
    fun `callbacks see the replacement and cannot recursively replace it`() {
        val first = manager.getSessionId()
        val events = mutableListOf<String>()
        manager.addObserver(
            object : SessionObserver {
                override fun onSessionEnded(session: Session) {
                    assertThat(session.id).isEqualTo(first)
                    assertThat(manager.getSessionId()).isNotEmpty().isNotEqualTo(first)
                    assertThat(extension.startNewSession()).isFalse()
                    events.add("end")
                }

                override fun onSessionStarted(
                    newSession: Session,
                    previousSession: Session,
                ) {
                    assertThat(previousSession.id).isEqualTo(first)
                    assertThat(manager.getSessionId()).isEqualTo(newSession.id)
                    assertThat(storage.get()).isSameAs(newSession)
                    assertThat(extension.startNewSession()).isFalse()
                    events.add("start")
                }
            },
        )
        assertThat(extension.startNewSession()).isTrue()
        assertThat(events).containsExactly("end", "start")
    }

    @Test
    fun `concurrent request returns busy while the replacement remains readable`() {
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
        val executor = Executors.newFixedThreadPool(2)
        try {
            val accepted = executor.submit<Boolean> { extension.startNewSession() }
            assertThat(entered.await(5, SECONDS)).isTrue()
            val second = manager.getSessionId()
            assertThat(second).isNotEmpty().isNotEqualTo(first)
            assertThat(executor.submit<Boolean> { extension.startNewSession() }.get(1, SECONDS)).isFalse()
            assertThat(manager.getSessionId()).isEqualTo(second)
            release.countDown()
            assertThat(accepted.get(5, SECONDS)).isTrue()
            assertThat(extension.startNewSession()).isTrue()
            assertThat(manager.getSessionId()).isNotEqualTo(second)
        } finally {
            release.countDown()
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue()
        }
    }

    @Test
    fun `extension only replaces its own manager session`() {
        val other = SessionManager(clock, timeoutHandler = SessionIdTimeoutHandler(clock, 15.minutes), maxSessionLifetime = 4.hours)
        val otherId = other.getSessionId()
        assertThat(extension.startNewSession()).isTrue()
        assertThat(other.getSessionId()).isEqualTo(otherId)
    }

    @Test
    fun `no-op rum has no session extension`() {
        assertThat(RumBuilder.noop().getExtension(SessionExtension::class.java)).isNull()
    }
}
