/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.extensions.ApiExtension

/**
 * Manual session control for the built-in agent session manager.
 *
 * Obtain it with `rum.getExtension(SessionExtension::class.java)`. It is unavailable on
 * RUM instances that do not register this extension, including the no-op implementation.
 */
@Incubating
interface SessionExtension : ApiExtension {
    override val type: Class<out ApiExtension>
        get() = SessionExtension::class.java

    /**
     * Ends the current session and immediately starts a new one, linked to the previous session.
     * There is no gap without a current session. Existing telemetry keeps its original session ID;
     * subsequent telemetry uses the new session. Configured expiry rules still apply.
     *
     * Returns true after saving the new session and notifying observers. Returns false if another
     * transition is still being delivered, including calls from a session observer. The request is
     * not queued; callers may retry after that transition finishes.
     *
     * Storage and observer callbacks run synchronously on the calling thread. If one throws, its
     * exception propagates and the new in-memory session may already be active.
     */
    fun startNewSession(): Boolean
}
