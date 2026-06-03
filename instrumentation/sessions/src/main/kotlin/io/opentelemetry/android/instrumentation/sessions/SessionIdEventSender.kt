/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.SessionAttributes.SESSION_ID
import io.opentelemetry.kotlin.semconv.SessionAttributes.SESSION_PREVIOUS_ID

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
        @OptIn(IncubatingApi::class)
        val eventBuilder =
            eventLogger
                .logRecordBuilder()
                .setEventName(EVENT_SESSION_START)
                .setAttribute(SESSION_ID, newSession.id)
        val previousSessionId = previousSession.id
        if (previousSessionId.isNotEmpty()) {
            @OptIn(IncubatingApi::class)
            eventBuilder.setAttribute(SESSION_PREVIOUS_ID, previousSessionId)
        }
        eventBuilder.emit()
    }

    override fun onSessionEnded(session: Session) {
        if (session.id.isEmpty()) {
            return
        }
        @OptIn(IncubatingApi::class)
        eventLogger
            .logRecordBuilder()
            .setEventName(EVENT_SESSION_END)
            .setAttribute(SESSION_ID, session.id)
            .emit()
    }

    companion object {
        const val EVENT_SESSION_START: String = "session.start"
        const val EVENT_SESSION_END: String = "session.end"
    }
}
