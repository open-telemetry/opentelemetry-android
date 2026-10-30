/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.IncubatingOpenTelemetryRum
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.session.SessionManager

@OptIn(Incubating::class)
internal class IncubatingOpenTelemetryRumImpl(
    private val delegate: OpenTelemetryRum,
    private val ownedSessionManager: SessionManager?,
) : IncubatingOpenTelemetryRum,
    OpenTelemetryRum by delegate {
    override fun startNewSession(): Boolean =
        checkNotNull(ownedSessionManager) {
            "startNewSession() requires the default session provider created by the initializer."
        }.resetSession()
}
