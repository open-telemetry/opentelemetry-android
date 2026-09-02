/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating

/**
 * Facade over [SessionIdTimeoutHandler], which is internal to the agent and created during
 * initialization. Because of that, this extension is registered explicitly through the RUM builder
 * rather than discovered from the classpath.
 */
@OptIn(Incubating::class)
internal class SessionActivityApiImpl(
    private val timeoutHandler: SessionIdTimeoutHandler,
) : SessionActivityApi {
    override fun userActive() {
        timeoutHandler.onUserActive()
    }

    override fun userInactive() {
        timeoutHandler.onUserInactive()
    }
}
