/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.common.RumConstants.Events.EVENT_SESSION_END;
import static io.opentelemetry.android.common.RumConstants.Events.EVENT_SESSION_START;
import static io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_ID;
import static io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_PREVIOUS_ID;

import androidx.annotation.NonNull;

import io.opentelemetry.android.session.Session;
import io.opentelemetry.android.session.SessionObserver;
import io.opentelemetry.api.incubator.events.EventBuilder;
import io.opentelemetry.api.incubator.events.EventLogger;

final class SessionIdEventSender implements SessionObserver {

    private final EventLogger eventLogger;

    SessionIdEventSender(EventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    @Override
    public void onSessionStarted(@NonNull Session newSession, @NonNull Session previousSession) {
        EventBuilder eventBuilder = eventLogger
                .builder(EVENT_SESSION_START)
                .put(SESSION_ID, newSession.getId());
        String previousSessionId = previousSession.getId();
        if(!previousSessionId.isEmpty()){
            eventBuilder.put(SESSION_PREVIOUS_ID, previousSessionId);
        }
        eventBuilder.emit();
    }

    @Override
    public void onSessionEnded(@NonNull Session session) {
        if(session.getId().isEmpty()) {
            return;
        }
        eventLogger.builder(EVENT_SESSION_END).put(SESSION_ID, session.getId()).emit();
    }
}
