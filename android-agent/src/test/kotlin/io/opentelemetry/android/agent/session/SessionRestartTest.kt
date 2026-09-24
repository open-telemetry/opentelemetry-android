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
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS

@OptIn(Incubating::class)
class SessionRestartTest {
    private val clock = TestClock.create()

    @Test
    fun `opted-in startup reads before saving and links without ending the previous process session`() {
        val storage = mockk<SessionStorage>()
        val previous = SessionImpl("a".repeat(32), clock.now())
        every { storage.get() } returns previous
        every { storage.save(any()) } returns Unit
        val observer = mockk<SessionObserver>(relaxed = true)
        // The previous timestamp is a link, not resumed lifetime state.
        clock.advance(1, DAYS)
        val config = SessionConfig(linkPreviousSessionOnRestart = true)
        val manager = SessionManager.create(SessionIdTimeoutHandler(config, clock), config, clock, storage)
        manager.addObserver(observer)
        verify(exactly = 1) { storage.get() }
        verify(exactly = 0) { storage.save(any()) }
        val first = manager.getSessionId()
        assertThat(first).isNotEqualTo(previous.id)
        assertThat(manager.getSessionId()).isEqualTo(first)
        clock.advance(4, HOURS)
        val second = manager.getSessionId()
        verifyOrder {
            storage.get()
            storage.save(match { it.id == first })
            observer.onSessionStarted(match { it.id == first }, match { it.id == previous.id })
            storage.save(match { it.id == second })
            observer.onSessionEnded(match { it.id == first })
            observer.onSessionStarted(match { it.id == second }, match { it.id == first })
        }
        verify(exactly = 0) { observer.onSessionEnded(match { it.id == previous.id }) }
    }

    @Test
    fun `startup snapshots custom storage before it can mutate the previous record`() {
        var storedId = "a".repeat(32)
        val storage =
            object : SessionStorage {
                override fun get(): Session =
                    object : Session {
                        override val id: String get() = storedId
                        override val startTimestamp: Long = clock.now()
                    }

                override fun save(newSession: Session) {
                    storedId = newSession.id
                }
            }
        val observer = mockk<SessionObserver>(relaxed = true)
        val config = SessionConfig(linkPreviousSessionOnRestart = true)
        val manager = SessionManager.create(SessionIdTimeoutHandler(config, clock), config, clock, storage)
        manager.addObserver(observer)
        val id = manager.getSessionId()
        verify { observer.onSessionStarted(match { it.id == id }, match { it.id == "a".repeat(32) }) }
    }
}
