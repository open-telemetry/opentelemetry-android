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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(Incubating::class)
class PreviousSessionStorageTest {
    private val clock = TestClock.create()
    private val previous = SessionImpl("previous-launch", 12)
    private val events = mutableListOf<String>()
    private val ended = mutableListOf<Session>()
    private val links = mutableListOf<Session>()
    private val observer =
        object : SessionObserver {
            override fun onSessionEnded(session: Session) {
                ended.add(session)
            }

            override fun onSessionStarted(
                newSession: Session,
                previousSession: Session,
            ) {
                links.add(previousSession)
                events.add("start:${newSession.id}")
            }
        }

    @Test
    fun `reads before first overwrite without restoring or ending the old session`() {
        var stored: Session = previous
        val storage =
            object : SessionStorage {
                override fun get(): Session = error("Current storage must not be used for restoration")

                override fun getPreviousSession(): Session {
                    events.add("read")
                    return stored
                }

                override fun save(newSession: Session) {
                    events.add("save:${newSession.id}")
                    stored = newSession
                }
            }
        val manager = manager(storage)
        assertThat(events).isEmpty()
        val first = manager.getSessionId()
        assertThat(first).isNotEqualTo(previous.id)
        assertThat(stored.startTimestamp).isEqualTo(clock.now())
        assertThat(links.map { it.id }).containsExactly(previous.id)
        assertThat(links.single().startTimestamp).isEqualTo(previous.startTimestamp)
        assertThat(ended).containsExactly(invalidSession)
        assertThat(events).containsExactly("read", "save:$first", "start:$first")
        assertThat(manager.getSessionId()).isEqualTo(first)
        assertThat(manager.resetSession()).isTrue()
        val second = manager.getSessionId()
        assertThat(links.map { it.id }).containsExactly(previous.id, first)
        assertThat(events).containsExactly("read", "save:$first", "start:$first", "save:$second", "start:$second")
    }

    @Test
    fun `default and empty previous sessions leave startup unlinked`() {
        val stores =
            listOf(
                InMemorySessionStorage(previous),
                object : SessionStorage {
                    override fun get(): Session = previous

                    override fun save(newSession: Session) {}

                    override fun getPreviousSession(): Session = invalidSession
                },
            )
        for (storage in stores) {
            val manager = manager(storage)
            assertThat(manager.resetSession()).isTrue()
            assertThat(manager.getSessionId()).isNotEqualTo(previous.id)
        }
        assertThat(links).containsExactly(invalidSession, invalidSession)
        assertThat(ended).containsExactly(invalidSession, invalidSession)
    }

    @Test
    fun `slow getter permits concurrent reads and rejects reentrant and concurrent resets`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        lateinit var manager: SessionManager
        var getterCalls = 0
        val storage =
            object : SessionStorage {
                override fun get(): Session = error("unused")

                override fun save(newSession: Session) {}

                override fun getPreviousSession(): Session {
                    getterCalls++
                    assertThat(manager.getSessionId()).isNotEmpty()
                    assertThat(manager.resetSession()).isFalse()
                    entered.countDown()
                    check(release.await(5, SECONDS))
                    return previous
                }
            }
        manager = manager(storage)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first = pool.submit<String> { manager.getSessionId() }
            assertThat(entered.await(5, SECONDS)).isTrue()
            val concurrent = pool.submit<String> { manager.getSessionId() }.get(1, SECONDS)
            assertThat(pool.submit<Boolean> { manager.resetSession() }.get(1, SECONDS)).isFalse()
            assertThat(links).isEmpty()
            release.countDown()
            assertThat(first.get(5, SECONDS)).isEqualTo(concurrent)
            assertThat(links.map { it.id }).containsExactly(previous.id)
            assertThat(getterCalls).isEqualTo(1)
        } finally {
            release.countDown()
            pool.shutdownNow()
            assertThat(pool.awaitTermination(5, SECONDS)).isTrue()
        }
    }

    @Test
    fun `a throwing getter releases the transition without retrying the previous lookup`() {
        var calls = 0
        val storage =
            object : SessionStorage {
                override fun get(): Session = error("unused")

                override fun save(newSession: Session) {}

                override fun getPreviousSession(): Session {
                    calls++
                    error("storage failure")
                }
            }
        val manager = manager(storage)
        assertThatThrownBy { manager.getSessionId() }.hasMessage("storage failure")
        assertThat(links).isEmpty()
        val first = manager.getSessionId()
        assertThat(manager.resetSession()).isTrue()
        assertThat(links.map { it.id }).containsExactly(first)
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `snapshots a storage view before save changes its values`() {
        val view =
            object : Session {
                override var id = previous.id
                override var startTimestamp = previous.startTimestamp
            }
        val storage =
            object : SessionStorage {
                override fun get(): Session = view

                override fun getPreviousSession(): Session = view

                override fun save(newSession: Session) {
                    view.id = newSession.id
                    view.startTimestamp = newSession.startTimestamp
                }
            }
        manager(storage).getSessionId()
        assertThat(links.single().id).isEqualTo(previous.id)
        assertThat(links.single().startTimestamp).isEqualTo(previous.startTimestamp)
    }

    private fun manager(storage: SessionStorage): SessionManager =
        SessionManager(clock, storage, SessionIdTimeoutHandler(clock, 15.minutes), maxSessionLifetime = 4.hours)
            .also { it.addObserver(observer) }
}
