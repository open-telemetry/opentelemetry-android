/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import io.opentelemetry.android.semconv.events.SessionEndEvent
import io.opentelemetry.android.semconv.events.SessionStartEvent
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.api.logs.Logger

/**
 * This class is responsible for generating the session related events as
 * specified in the OpenTelemetry semantic conventions.
 */
internal class SessionIdEventSender(
    private val eventLogger: Logger,
) : SessionObserver {
    override fun onSessionStarted(
        newSession: Session,
        previousSession: Session,
    ) {
        val previousSessionId = previousSession.id
        SessionStartEvent(
            sessionId = newSession.id,
            sessionPreviousId = previousSessionId.ifEmpty { null },
        ).emit(eventLogger)
    }

    override fun onSessionEnded(session: Session) {
        if (session.id.isEmpty()) {
            return
        }
        SessionEndEvent(sessionId = session.id).emit(eventLogger)
    }
}
