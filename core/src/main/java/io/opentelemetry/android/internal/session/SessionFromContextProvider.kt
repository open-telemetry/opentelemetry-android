/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.session

import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.context.Context

/**
 * A SessionProvider instance that requires the session to be stored
 * in the current otel Context.
 */
class SessionFromContextProvider : SessionProvider {
    override fun getSessionId(): String {
        val session: Session = Context.current().get(SESSION_CONTEXT_KEY) as Session
        return session.getId()
    }
}
