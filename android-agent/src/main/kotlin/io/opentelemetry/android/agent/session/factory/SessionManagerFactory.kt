/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session.factory

import android.app.Application
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.agent.session.SessionManager
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider

/**
 * A [SessionProviderFactory] that creates a [SessionManager].
 *
 * This interface is the interface for the factory in the Factory design pattern.
 * @see <a href="https://www.tutorialspoint.com/design_pattern/factory_pattern.htm">Factory Design Pattern</a>
 */
@OptIn(Incubating::class)
open class SessionManagerFactory : SessionProviderFactory {
    /**
     * Creates a [SessionManager] with the [application] and [sessionConfig].
     * @param application for watching application states.
     * @param sessionConfig for configuring the session management.
     * @return the newly created provider.
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
