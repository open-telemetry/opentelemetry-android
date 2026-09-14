/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.android.session.SessionPublisher
import io.opentelemetry.sdk.common.Clock
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random
import kotlin.time.Duration

@OptIn(Incubating::class)
internal class SessionManager(
    private val clock: Clock,
    private val sessionStorage: SessionStorage = InMemorySessionStorage(),
    private val timeoutHandler: SessionIdTimeoutHandler,
    private val idGenerator: SessionIdGenerator = DefaultSessionIdGenerator(Random.Default),
    private val maxSessionLifetime: Duration,
) : SessionProvider,
    SessionPublisher {
    private val lock = Any()

    @Volatile
    private var session: Session = invalidSession

    @Volatile
    private var transitionInProgress = false

    private val observers = CopyOnWriteArrayList<SessionObserver>()

    override fun addObserver(observer: SessionObserver) {
        observers.add(observer)
    }

    override fun getSessionId(): String {
        val currentSession = session
        if (!transitionInProgress && !sessionHasExpired(currentSession) && !timeoutHandler.hasTimedOut()) {
            timeoutHandler.bump()
            return currentSession.id
        }
        return synchronized(lock) {
            val latestSession = session
            if (sessionHasExpired(latestSession) || timeoutHandler.hasTimedOut()) {
                val newId = idGenerator.generateSessionId()
                val newSession = SessionImpl(newId, clock.now())
                // Readers must wait until storage and observers finish the transition.
                transitionInProgress = true
                try {
                    session = newSession
                    // Storage and observers may record telemetry that reads the session again.
                    timeoutHandler.bump()
                    sessionStorage.save(newSession)
                    notifyObserversOfSessionUpdate(latestSession, newSession)
                } finally {
                    transitionInProgress = false
                }
                newSession.id
            } else {
                timeoutHandler.bump()
                latestSession.id
            }
        }
    }

    private fun notifyObserversOfSessionUpdate(
        currentSession: Session,
        newSession: Session,
    ) {
        observers.forEach {
            it.onSessionEnded(currentSession)
            it.onSessionStarted(newSession, currentSession)
        }
    }

    private fun sessionHasExpired(session: Session): Boolean {
        val elapsedTime = clock.now() - session.startTimestamp
        return elapsedTime >= maxSessionLifetime.inWholeNanoseconds
    }

    companion object {
        @OptIn(Incubating::class)
        @JvmStatic
        fun create(
            timeoutHandler: SessionIdTimeoutHandler,
            sessionConfig: SessionConfig,
            clock: Clock,
            sessionStorage: SessionStorage = InMemorySessionStorage(),
        ): SessionManager =
            SessionManager(
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = sessionConfig.maxLifetime,
                clock = clock,
                sessionStorage = sessionStorage,
            )
    }
}
