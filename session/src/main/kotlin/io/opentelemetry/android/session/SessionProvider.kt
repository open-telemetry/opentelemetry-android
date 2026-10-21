/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

/**
 * Provides information about the current session.
 */
fun interface SessionProvider {
    /**
     * Retrieves the current session ID.
     */
    fun getSessionId(): String

    /**
     * Immediately replaces the current session with a new one linked to the previous session.
     * Returns true when replacement completes, or false when unsupported or a transition is busy.
     * A false result must leave the session unchanged and must not queue a replacement.
     * Implementations may propagate storage or observer failures after changing the session.
     *
     * The default implementation does nothing, so existing providers need not support replacement.
     */
    fun startNewSession(): Boolean = false

    companion object {
        /**
         * A no-op implementation of [SessionProvider].
         */
        @JvmStatic
        fun getNoop(): SessionProvider = NO_OP

        private val NO_OP: SessionProvider by lazy {
            SessionProvider { "" }
        }
    }
}
