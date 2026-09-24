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
    private val previousProcessSession: Session = invalidSession,
) : SessionProvider,
    SessionPublisher {
    private val lock = Any()

    @Volatile
    private var session: Session = invalidSession

    private var transitionInProgress = false

    private val observers = CopyOnWriteArrayList<SessionObserver>()

    override fun addObserver(observer: SessionObserver) {
        observers.add(observer)
    }

    override fun getSessionId(): String {
        val currentSession = session
        if (!sessionHasExpired(currentSession) && !timeoutHandler.hasTimedOut()) {
            timeoutHandler.bump()
            return currentSession.id
        }
        val previousSession: Session
        val newSession: Session
        synchronized(lock) {
            previousSession = session
            // Do not clear an expired inactivity timer while its transition is deferred.
            if (transitionInProgress) return previousSession.id
            if (!sessionHasExpired(previousSession) && !timeoutHandler.hasTimedOut()) {
                timeoutHandler.bump()
                return previousSession.id
            }
            newSession = SessionImpl(idGenerator.generateSessionId(), clock.now())
            timeoutHandler.bump()
            session = newSession
            transitionInProgress = true
        }
        try {
            // Keep saves and notifications ordered without making readers wait for those calls.
            sessionStorage.save(newSession)
            notifyObserversOfSessionUpdate(previousSession, newSession)
        } finally {
            synchronized(lock) { transitionInProgress = false }
        }
        return newSession.id
    }

    private fun notifyObserversOfSessionUpdate(
        currentSession: Session,
        newSession: Session,
    ) {
        observers.forEach {
            it.onSessionEnded(currentSession)
            it.onSessionStarted(newSession, if (currentSession === invalidSession) previousProcessSession else currentSession)
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
                previousProcessSession =
                    if (sessionConfig.linkPreviousSessionOnRestart) {
                        sessionStorage.get().let { SessionImpl(it.id, it.startTimestamp) }
                    } else {
                        invalidSession
                    },
            )
    }
}
