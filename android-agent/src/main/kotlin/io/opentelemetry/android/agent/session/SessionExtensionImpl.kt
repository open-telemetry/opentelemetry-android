/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating

@OptIn(Incubating::class)
internal class SessionExtensionImpl(
    private val sessionManager: SessionManager,
) : SessionExtension {
    override fun startNewSession(): Boolean = sessionManager.resetSession()
}
