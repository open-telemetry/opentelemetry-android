/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

/**
 * A facade for retrieving session identifiers from a [SessionProvider].
 *
 * @param sessionProvider the [SessionProvider] used to retrieve session identifiers.
 */
class SessionIdentifierFacade(
    private val sessionProvider: SessionProvider,
) : SessionIdFacade {
    /**
     * the current session identifiers.
     *
     * Retrieves both the current session ID and previous session ID from the underlying
     * [SessionProvider]. The session provider is queried on each access to ensure fresh
     * session data, as session IDs can change during the application lifecycle.
     */
    override val sessionIdentifiers: SessionIdentifiers
        get() =
            SessionIdentifiers(
                currentSessionId = sessionProvider.getSessionId(),
                previousSessionId = sessionProvider.getPreviousSessionId(),
            )
}
