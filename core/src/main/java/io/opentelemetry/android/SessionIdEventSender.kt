/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.android

import io.opentelemetry.android.common.RumConstants.Events.EVENT_SESSION_END
import io.opentelemetry.android.common.RumConstants.Events.EVENT_SESSION_START
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.api.incubator.events.EventLogger
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_ID
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_PREVIOUS_ID

internal class SessionIdEventSender(private val eventLogger: EventLogger) : SessionObserver {

    override fun onSessionStarted(newSession: Session, previousSession: Session) {
        val eventBuilder = eventLogger
            .builder(EVENT_SESSION_START)
            .put(SESSION_ID, newSession.getId())
        val previousSessionId = previousSession.getId()
        if (previousSessionId.isNotEmpty()) {
            eventBuilder.put(SESSION_PREVIOUS_ID, previousSessionId)
        }
        eventBuilder.emit()
    }

    override fun onSessionEnded(session: Session) {
        if (session.getId().isEmpty()) {
            return
        }
        eventLogger.builder(EVENT_SESSION_END)
            .put(SESSION_ID, session.getId())
            .emit()
    }
}
