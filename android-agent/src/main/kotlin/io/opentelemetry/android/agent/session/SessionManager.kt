/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.common.Clock
import java.util.Collections.synchronizedList
import java.util.concurrent.TimeUnit

internal class SessionManager(
    private val clock: Clock = Clock.getDefault(),
    private val sessionStorage: SessionStorage = SessionStorage.InMemory(),
    private val timeoutHandler: SessionIdTimeoutHandler,
    private val idGenerator: SessionIdGenerator = SessionIdGenerator.DEFAULT,
    private val sessionLifetimeNanos: Long = TimeUnit.HOURS.toNanos(4),
) : SessionProvider, SessionPublisher {
    // TODO: Make thread safe / wrap with AtomicReference?
    private var session: Session = Session.NONE
    private val observers = synchronizedList(ArrayList<SessionObserver>())

    init {
        sessionStorage.save(session)
    }

    override fun addObserver(observer: SessionObserver) {
        observers.add(observer)
    }

    override fun getSessionId(): String {
        // value will never be null
        var newSession = session

        if (sessionHasExpired() || timeoutHandler.hasTimedOut()) {
            val newId = idGenerator.generateSessionId()

            // TODO FIXME: This is not threadsafe -- if two threads call getSessionId()
            // at the same time while timed out, two new sessions are created
            // Could require SessionStorage impls to be atomic/threadsafe or
            // do the locking in this class?

            newSession = Session.DefaultSession(newId, clock.now())
            sessionStorage.save(newSession)
        }

        timeoutHandler.bump()

        // observers need to be called after bumping the timer because it may
        // create a new span
        if (newSession != session) {
            observers.forEach {
                it.onSessionEnded(session)
                it.onSessionStarted(newSession, session)
            }
            session = newSession
        }
        return session.getId()
    }

    private fun sessionHasExpired(): Boolean {
        val elapsedTime = clock.now() - session.getStartTimestamp()
        return elapsedTime >= sessionLifetimeNanos
    }

    companion object {
        @JvmStatic
        fun create(
            timeoutHandler: SessionIdTimeoutHandler,
            sessionLifetimeNanos: Long,
        ): SessionManager {
            return SessionManager(
                timeoutHandler = timeoutHandler,
                sessionLifetimeNanos = sessionLifetimeNanos,
            )
        }
    }
}
