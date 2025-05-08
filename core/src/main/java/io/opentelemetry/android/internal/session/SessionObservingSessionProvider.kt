/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.session

import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.android.session.SessionProvider
import java.util.concurrent.atomic.AtomicReference

/**
 * A SessionObserver that listens for session start events, stores the session,
 * and is also a SessionProvider, that can return the session id.
 */
internal class SessionObservingSessionProvider :
    SessionProvider,
    SessionObserver {
    private val session = AtomicReference<Session>(Session.NONE)

    override fun getSessionId(): String = session.get().getId()

    override fun onSessionStarted(
        newSession: Session,
        previousSession: Session,
    ) = session.set(newSession)
}
