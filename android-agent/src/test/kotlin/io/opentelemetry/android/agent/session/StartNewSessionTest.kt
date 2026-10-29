/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.IncubatingOpenTelemetryRum
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.RumBuilder
import io.opentelemetry.android.agent.IncubatingOpenTelemetryRumImpl
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.testing.time.TestClock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(Incubating::class)
class StartNewSessionTest {
    private val clock = TestClock.create()
    private val storage = InMemorySessionStorage()
    private val timeout = SessionIdTimeoutHandler(clock, 15.minutes)
    private val manager = SessionManager(clock, storage, timeout, maxSessionLifetime = 4.hours)
    private val rum = rum(manager)

    @Test
    fun `existing rum operations still use the original instance`() {
        val delegate = mockk<OpenTelemetryRum>(relaxed = true)
        val telemetry = OpenTelemetry.noop()
        every { delegate.openTelemetry } returns telemetry
        every { delegate.sessionProvider } returns manager
        every { delegate.clock } returns clock
        val wrapped = IncubatingOpenTelemetryRumImpl(delegate, manager)
        assertThat(wrapped.openTelemetry).isSameAs(telemetry)
        assertThat(wrapped.sessionProvider).isSameAs(manager)
        assertThat(wrapped.clock).isSameAs(clock)
        val attributes = Attributes.builder().put("test", "value").build()
        wrapped.emitEvent("test-event", "test-body", attributes)
        wrapped.shutdown()
        verify(exactly = 1) { delegate.emitEvent("test-event", "test-body", attributes) }
        verify(exactly = 1) { delegate.shutdown() }
    }

    @Test
    fun `forwards the result without retrying a busy transition`() {
        for (accepted in listOf(true, false)) {
            val provider = mockk<SessionManager>()
            every { provider.resetSession() } returns accepted
            assertThat(rum(provider).startNewSession()).isEqualTo(accepted)
            verify(exactly = 1) { provider.resetSession() }
        }
    }

    @Test
    fun `does not retry or hide a partially delivered transition`() {
        val provider = mockk<SessionManager>()
        val failure = IllegalStateException("observer failed")
        every { provider.resetSession() } throws failure
        assertThatThrownBy { rum(provider).startNewSession() }.isSameAs(failure)
        verify(exactly = 1) { provider.resetSession() }
    }

    @Test
    fun `first manual start creates a session without a telemetry lookup`() {
        assertThat(rum.startNewSession()).isTrue()
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
                    assertThat(rum.startNewSession()).isFalse()
                    events.add("end")
                }

                override fun onSessionStarted(
                    newSession: Session,
                    previousSession: Session,
                ) {
                    assertThat(previousSession.id).isEqualTo(first)
                    assertThat(manager.getSessionId()).isEqualTo(newSession.id)
                    assertThat(storage.get()).isSameAs(newSession)
                    assertThat(rum.startNewSession()).isFalse()
                    events.add("start")
                }
            },
        )
        assertThat(rum.startNewSession()).isTrue()
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
            val accepted = executor.submit<Boolean> { rum.startNewSession() }
            assertThat(entered.await(5, SECONDS)).isTrue()
            val second = manager.getSessionId()
            assertThat(second).isNotEmpty().isNotEqualTo(first)
            assertThat(executor.submit<Boolean> { rum.startNewSession() }.get(1, SECONDS)).isFalse()
            assertThat(manager.getSessionId()).isEqualTo(second)
            release.countDown()
            assertThat(accepted.get(5, SECONDS)).isTrue()
            assertThat(rum.startNewSession()).isTrue()
            assertThat(manager.getSessionId()).isNotEqualTo(second)
        } finally {
            release.countDown()
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue()
        }
    }

    @Test
    fun `rum only replaces its own manager session`() {
        val other = SessionManager(clock, timeoutHandler = SessionIdTimeoutHandler(clock, 15.minutes), maxSessionLifetime = 4.hours)
        val otherId = other.getSessionId()
        assertThat(rum.startNewSession()).isTrue()
        assertThat(other.getSessionId()).isEqualTo(otherId)
    }

    @Test
    fun `end observer failure releases the transition without retrying it`() = checkObserverFailure(failOnEnd = true)

    @Test
    fun `start observer failure releases the transition without retrying it`() = checkObserverFailure(failOnEnd = false)

    private fun checkObserverFailure(failOnEnd: Boolean) {
        manager.getSessionId()
        var fail = true
        val failure = IllegalStateException("observer failed")
        val events = mutableListOf<String>()
        val laterObserver = mockk<SessionObserver>(relaxed = true)
        manager.addObserver(
            object : SessionObserver {
                override fun onSessionEnded(session: Session) {
                    events.add("end:${session.id}")
                    if (fail && failOnEnd) throw failure
                }

                override fun onSessionStarted(
                    newSession: Session,
                    previousSession: Session,
                ) {
                    events.add("start:${newSession.id}:${previousSession.id}")
                    if (fail) throw failure
                }
            },
        )
        manager.addObserver(laterObserver)
        val first = manager.getSessionId()
        assertThatThrownBy { rum.startNewSession() }.isSameAs(failure)
        val second = manager.getSessionId()
        assertThat(second).isNotEqualTo(first).isEqualTo(storage.get().id)
        val expected = if (failOnEnd) listOf("end:$first") else listOf("end:$first", "start:$second:$first")
        assertThat(events).containsExactlyElementsOf(expected)
        verify(exactly = 0) { laterObserver.onSessionEnded(any()) }
        verify(exactly = 0) { laterObserver.onSessionStarted(any(), any()) }
        fail = false
        events.clear()
        assertThat(rum.startNewSession()).isTrue()
        val third = manager.getSessionId()
        assertThat(events).containsExactly("end:$second", "start:$third:$second")
        verify(exactly = 1) { laterObserver.onSessionStarted(match { it.id == third }, match { it.id == second }) }
    }

    @Test
    fun `busy reset during storage does not extend the session timeout`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val storage = mockk<SessionStorage>()
        every { storage.getPreviousSession() } returns null
        every { storage.save(any()) } answers {
            entered.countDown()
            check(release.await(5, SECONDS))
        }
        val timeout = SessionIdTimeoutHandler(clock, 15.minutes, 1.seconds)
        val manager = SessionManager(clock, storage, timeout, maxSessionLifetime = 4.hours)
        val rum = rum(manager)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val accepted = executor.submit<Boolean> { rum.startNewSession() }
            assertThat(entered.await(5, SECONDS)).isTrue()
            clock.advance(900, MILLISECONDS)
            assertThat(rum.startNewSession()).isFalse()
            verify(exactly = 1) { storage.save(any()) }
            clock.advance(100, MILLISECONDS)
            assertThat(timeout.hasTimedOut()).isTrue()
            release.countDown()
            assertThat(accepted.get(5, SECONDS)).isTrue()
            manager.getSessionId()
            verify(exactly = 2) { storage.save(any()) }
        } finally {
            release.countDown()
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue()
        }
    }

    @Test
    fun `no-op rum does not start a session`() {
        assertThat(IncubatingOpenTelemetryRumImpl(RumBuilder.noop(), null).startNewSession()).isFalse()
        assertThat(RumBuilder.noop().sessionProvider.getSessionId()).isEmpty()
    }

    @Test
    fun `wrapper without an owned manager does not consult its delegate`() {
        val delegate = mockk<OpenTelemetryRum>()
        assertThat(IncubatingOpenTelemetryRumImpl(delegate, null).startNewSession()).isFalse()
        verify { delegate wasNot Called }
    }

    private fun rum(provider: SessionManager): IncubatingOpenTelemetryRum =
        IncubatingOpenTelemetryRumImpl(
            object : OpenTelemetryRum {
                override val openTelemetry = OpenTelemetry.noop()
                override val sessionProvider = provider
                override val clock = this@StartNewSessionTest.clock

                override fun emitEvent(
                    eventName: String,
                    body: String,
                    attributes: Attributes,
                ) {}

                override fun shutdown() {}
            },
            provider,
        )
}
