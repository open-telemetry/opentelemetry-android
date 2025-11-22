/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session.factory

import android.app.Application
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.session.SessionProvider

/**
 * Factory for creating [SessionProvider] instances.
 *
 * This interface follows the Factory design pattern to enable flexible dependency injection
 * and testing of session management components. Implementations can provide custom session
 * providers with different behaviors while maintaining a consistent creation interface.
 *
 * @see SessionManagerFactory
 * @see SessionProvider
 */
interface SessionProviderFactory {
    /**
     * Creates a session provider instance.
     *
     * @param application the Android application instance.
     * @param sessionConfig the configuration for session management.
     * @return a session provider instance.
     */
    fun createSessionProvider(
        application: Application,
        sessionConfig: SessionConfig,
    ): SessionProvider
}
