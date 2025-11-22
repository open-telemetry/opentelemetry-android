/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.android.session.SessionPublisher
import io.opentelemetry.sdk.common.Clock
import java.util.Collections.synchronizedList
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration

internal class SessionManager(
    private val clock: Clock = Clock.getDefault(),
    private val sessionStorage: SessionStorage = InMemorySessionStorage(),
    private val timeoutHandler: SessionIdTimeoutHandler,
    private val idGenerator: SessionIdGenerator = DefaultSessionIdGenerator(Random.Default),
    private val maxSessionLifetime: Duration,
) : SessionProvider,
    SessionPublisher {
    private var session: AtomicReference<Session> = AtomicReference(Session.NONE)
    private var previousSession: AtomicReference<Session> = AtomicReference(Session.NONE)
    private val observers = synchronizedList(ArrayList<SessionObserver>())

    init {
        sessionStorage.save(session.get())
    }

    override fun addObserver(observer: SessionObserver) {
        observers.add(observer)
    }

    override fun getSessionId(): String {
        val currentSession = session.get()

        // Check if we need to create a new session.
        return if (sessionHasExpired() || timeoutHandler.hasTimedOut()) {
            val newId = idGenerator.generateSessionId()
            val newSession = Session.DefaultSession(newId, clock.now())

            // Atomically update the session only if it hasn't been changed by another thread.
            if (session.compareAndSet(currentSession, newSession)) {
                sessionStorage.save(newSession)
                timeoutHandler.bump()
                // Track the previous session for session transition correlation
                previousSession.set(currentSession)
                // Observers need to be called after bumping the timer because it may create a new
                // span.
                notifyObserversOfSessionUpdate(currentSession, newSession)
                newSession.getId()
            } else {
                // Another thread accessed this function prior to creating a new session. Use the
                // current session.
                timeoutHandler.bump()
                session.get().getId()
            }
        } else {
            // No new session needed, just bump the timeout and return current session ID
            timeoutHandler.bump()
            currentSession.getId()
        }
    }

    override fun getPreviousSessionId(): String = previousSession.get().getId()

    private fun notifyObserversOfSessionUpdate(
        currentSession: Session,
        newSession: Session,
    ) {
        observers.forEach {
            it.onSessionEnded(currentSession)
            it.onSessionStarted(newSession, currentSession)
        }
    }

    private fun sessionHasExpired(): Boolean {
        val elapsedTime = clock.now() - session.get().getStartTimestamp()
        return elapsedTime >= maxSessionLifetime.inWholeNanoseconds
    }

    companion object {
        @OptIn(Incubating::class)
        @JvmStatic
        fun create(
            timeoutHandler: SessionIdTimeoutHandler,
            sessionConfig: SessionConfig,
        ): SessionManager =
            SessionManager(
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = sessionConfig.maxLifetime,
            )
    }
}
