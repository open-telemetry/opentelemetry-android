/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session.factory

import android.app.Application
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.session.SessionProvider

/**
 * Creates a session Provider instance.
 *
 * This interface is the interface for the factory in the Factory design pattern.
 * @see <a href="https://www.tutorialspoint.com/design_pattern/factory_pattern.htm">Factory Design Pattern</a>
 */
@OptIn(Incubating::class)
interface SessionProviderFactory {
    /**
     * Creates a session Provider with the [application] and [sessionConfig].
     * @param application for watching application states.
     * @param sessionConfig for configuring the session management.
     * @return the newly created provider.
     */
    fun createSessionProvider(
        application: Application,
        sessionConfig: SessionConfig,
    ): SessionProvider
}
