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
import java.util.Collections.synchronizedList
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
            val previousSession = session
            session = newSession
            observers.forEach {
                it.onSessionEnded(previousSession)
                it.onSessionStarted(session, previousSession)
            }
        }
        return session.getId()
    }

    private fun sessionHasExpired(): Boolean {
        val elapsedTime = clock.now() - session.getStartTimestamp()
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
