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

internal class SessionManager(
    private val clock: Clock,
    private val sessionStorage: SessionStorage = InMemorySessionStorage(),
    private val timeoutHandler: SessionIdTimeoutHandler,
    private val idGenerator: SessionIdGenerator = DefaultSessionIdGenerator(Random.Default),
    private val maxSessionLifetime: Duration,
) : SessionProvider,
    SessionPublisher,
    SessionUserInteractionRecorder {
    @Volatile
    private var session: Session = invalidSession
    private val lock = Any()
    private var transitionInProgress = false
    private val observers = CopyOnWriteArrayList<SessionObserver>()

    init {
        sessionStorage.save(session)
    }

    override fun addObserver(observer: SessionObserver) {
        observers.add(observer)
    }

    // Lookup still creates or rotates a session, but never extends an existing session's inactivity.
    override fun getSessionId(): String = getSessionId(recordUserInteraction = false)

    override fun recordUserInteraction() {
        getSessionId(recordUserInteraction = true)
    }

    private fun getSessionId(recordUserInteraction: Boolean): String {
        val currentSession = session
        if (!recordUserInteraction && !sessionHasExpired(currentSession) && session === currentSession) {
            return currentSession.id
        }

        val previousSession: Session
        val newSession: Session
        var startedTransition = false
        try {
            synchronized(lock) {
                previousSession = session
                if (!sessionHasExpired(previousSession)) {
                    if (recordUserInteraction) {
                        timeoutHandler.bump()
                    }
                    return previousSession.id
                }
                // Finish the current notification sequence before allowing another rotation.
                if (transitionInProgress) {
                    return previousSession.id
                }
                newSession = SessionImpl(idGenerator.generateSessionId(), clock.now())
                startedTransition = true
                transitionInProgress = true
                session = newSession
                timeoutHandler.bump()
            }
            sessionStorage.save(newSession)
            notifyObserversOfSessionUpdate(previousSession, newSession)
            return newSession.id
        } finally {
            if (startedTransition) {
                synchronized(lock) {
                    transitionInProgress = false
                }
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
        if (session === invalidSession) {
            return true
        }
        val elapsedTime = clock.now() - session.startTimestamp
        return elapsedTime >= maxSessionLifetime.inWholeNanoseconds || timeoutHandler.hasTimedOut()
    }

    companion object {
        @OptIn(Incubating::class)
        @JvmStatic
        fun create(
            timeoutHandler: SessionIdTimeoutHandler,
            sessionConfig: SessionConfig,
            clock: Clock,
        ): SessionManager =
            SessionManager(
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = sessionConfig.maxLifetime,
                clock = clock,
            )
    }
}
