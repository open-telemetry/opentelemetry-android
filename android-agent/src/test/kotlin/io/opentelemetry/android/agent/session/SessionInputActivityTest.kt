/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.session.Session
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.testing.time.TestClock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@OptIn(Incubating::class)
class SessionInputActivityTest {
    private val clock = TestClock.create()
    private val config = SessionConfig(userInactivityTimeout = 1.seconds)
    private val timeout = SessionIdTimeoutHandler(config, clock)

    @Test
    fun `input burst keeps the exact last input deadline without saving per sample`() {
        val saved = mutableListOf<Session>()
        val storage =
            object : SessionStorage {
                override fun get(): Session = saved.lastOrNull() ?: invalidSession

                override fun save(newSession: Session) {
                    saved.add(newSession)
                }
            }
        val manager = SessionManager.create(timeout, config, clock, storage)
        manager.recordUserInteraction()
        val first = manager.getSessionId()
        repeat(120) {
            clock.advance(16, MILLISECONDS)
            manager.recordUserInteraction()
        }
        assertThat(saved).hasSize(1)
        clock.advance(999, MILLISECONDS)
        assertThat(manager.getSessionId()).isEqualTo(first)
        clock.advance(1, MILLISECONDS)
        manager.recordUserInteraction()
        assertThat(manager.getSessionId()).isNotEqualTo(first)
        assertThat(saved).hasSize(2)
    }

    @Test
    fun `continuous input cannot extend the maximum session lifetime`() {
        val manager = SessionManager(clock, timeoutHandler = timeout, maxSessionLifetime = 2.seconds)
        val first = manager.getSessionId()
        repeat(19) {
            clock.advance(100, MILLISECONDS)
            manager.recordUserInteraction()
            assertThat(manager.getSessionId()).isEqualTo(first)
        }
        clock.advance(100, MILLISECONDS)
        manager.recordUserInteraction()
        assertThat(manager.getSessionId()).isNotEqualTo(first)
    }

    @Test
    fun `active input does not wait for a lookup holding the manager lock`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val lookupThread = AtomicReference<Thread?>()
        val blockingClock =
            object : Clock by clock {
                override fun now(): Long {
                    if (lookupThread.compareAndSet(Thread.currentThread(), null)) {
                        entered.countDown()
                        check(release.await(5, SECONDS))
                    }
                    return clock.now()
                }
            }
        val manager = SessionManager(blockingClock, timeoutHandler = timeout, maxSessionLifetime = 4.hours)
        val first = manager.getSessionId()
        val executor = Executors.newFixedThreadPool(2)
        try {
            val lookup =
                executor.submit<String> {
                    lookupThread.set(Thread.currentThread())
                    manager.getSessionId()
                }
            assertThat(entered.await(5, SECONDS)).isTrue()
            clock.advance(900, MILLISECONDS)
            executor.submit { manager.recordUserInteraction() }.get(1, SECONDS)
            release.countDown()
            assertThat(lookup.get(5, SECONDS)).isEqualTo(first)
            clock.advance(999, MILLISECONDS)
            assertThat(manager.getSessionId()).isEqualTo(first)
            clock.advance(1, MILLISECONDS)
            assertThat(manager.getSessionId()).isNotEqualTo(first)
        } finally {
            release.countDown()
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue()
        }
    }
}
