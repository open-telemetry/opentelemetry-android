/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session.factory

import android.app.Application
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.agent.session.SessionManager
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider

/**
 * Default implementation of [SessionProviderFactory] that creates [SessionManager] instances.
 *
 * This implementation creates a [SessionManager] and wires it with:
 * - A timeout handler configured from the session config
 * - Application lifecycle integration for timeout management
 *
 * This class is open to allow custom implementations for specialized use cases.
 *
 * @see SessionProviderFactory
 * @see SessionManager
 * @see SessionConfig
 */
open class SessionManagerFactory : SessionProviderFactory {
    /**
     * Creates a session manager instance.
     *
     * This implementation creates a [SessionManager] with a timeout handler that is
     * registered with the application lifecycle service.
     *
     * @param application the Android application instance.
     * @param sessionConfig the configuration for session management.
     * @return a session manager instance.
     */
    override fun createSessionProvider(
        application: Application,
        sessionConfig: SessionConfig,
    ): SessionProvider {
        val timeoutHandler = SessionIdTimeoutHandler(sessionConfig)
        Services.get(application).appLifecycle.registerListener(timeoutHandler)
        return SessionManager.create(timeoutHandler, sessionConfig)
    }
}
