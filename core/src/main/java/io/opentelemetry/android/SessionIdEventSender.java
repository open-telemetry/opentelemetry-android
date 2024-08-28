/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_ID;
import static io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_PREVIOUS_ID;

import androidx.annotation.NonNull;
import io.opentelemetry.android.session.Session;
import io.opentelemetry.android.session.SessionObserver;
import io.opentelemetry.api.incubator.events.EventLogger;

final class SessionIdEventSender implements SessionObserver {

    private final EventLogger eventLogger;

    SessionIdEventSender(EventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    @Override
    public void onSessionStarted(@NonNull Session newSession, @NonNull Session previousSession) {
        // TODO: Use event name from semconv
        eventLogger
                .builder("session.start")
                .put(SESSION_ID, newSession.getId())
                .put(SESSION_PREVIOUS_ID, previousSession.getId())
                .emit();
    }

    @Override
    public void onSessionEnded(@NonNull Session session) {
        // TODO: Use event name from semconv
        eventLogger.builder("session.end").put(SESSION_ID, session.getId()).emit();
    }
}
