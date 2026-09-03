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
     *
     * The agent's built-in provider also records meaningful activity when this method is called.
     */
    fun getSessionId(): String

    /**
     * Retrieves the current session ID for passive telemetry attribution.
     *
     * Implementations that track activity in [getSessionId] should override this method so passive
     * telemetry cannot keep a session alive. The default delegates to [getSessionId] for
     * compatibility with existing custom providers.
     */
    fun getSessionIdForAttribution(): String = getSessionId()

    /**
     * Records meaningful activity for the current session.
     *
     * Implementations that maintain an inactivity window should override this method. The default
     * preserves existing custom-provider behavior by retrieving the current session ID.
     */
    fun recordActivity() {
        getSessionId()
    }

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
