/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.android.session.SessionPublisher
import io.opentelemetry.sdk.common.Clock
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
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
    ApplicationStateListener {
    private val session: AtomicReference<Session> = AtomicReference(invalidSession)
    private val observers = CopyOnWriteArrayList<SessionObserver>()
    private val accessLock = Any()

    init {
        sessionStorage.save(session.get())
    }

    override fun addObserver(observer: SessionObserver) {
        observers.add(observer)
    }

    override fun getSessionId(): String = accessSession(recordActivity = true)

    override fun getSessionIdForAttribution(): String = accessSession(recordActivity = false)

    override fun recordActivity() {
        accessSession(recordActivity = true)
    }

    override fun onApplicationForegrounded() {
        recordActivity()
    }

    override fun onApplicationBackgrounded() = Unit

    private fun accessSession(recordActivity: Boolean): String {
        var transition: Pair<Session, Session>? = null
        val sessionId =
            synchronized(accessLock) {
                val currentSession = session.get()
                if (sessionHasExpired(currentSession) || timeoutHandler.hasTimedOut()) {
                    val newSession = SessionImpl(idGenerator.generateSessionId(), clock.now())
                    session.set(newSession)
                    sessionStorage.save(newSession)
                    // A new session starts a new inactivity window even for passive attribution.
                    timeoutHandler.bump()
                    transition = currentSession to newSession
                    newSession.id
                } else {
                    if (recordActivity) {
                        timeoutHandler.bump()
                    }
                    currentSession.id
                }
            }

        transition?.let { (previousSession, newSession) ->
            notifyObserversOfSessionUpdate(previousSession, newSession)
        }
        return sessionId
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
        ): SessionManager =
            SessionManager(
                timeoutHandler = timeoutHandler,
                maxSessionLifetime = sessionConfig.maxLifetime,
                clock = clock,
            )
    }
}
