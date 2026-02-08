/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

/**
 * Provides session identifiers for tracking user sessions in telemetry data.
 *
 * Sessions provide a way to group related telemetry data (spans, logs, metrics) that occur during
 * a logical user interaction or application usage period.
 */
interface SessionProvider {
    /**
     * Returns the current session ID.
     *
     * @return the current session ID string, or [NO_SESSION_ID] if no session is active.
     */
    fun getSessionId(): String

    /**
     * Returns the previous session ID if a session transition occurred.
     *
     * This is useful for correlating telemetry data across session boundaries,
     * allowing observability systems to understand the flow between sessions.
     *
     * @return the previous session ID string, or [NO_SESSION_ID] if there is no previous session.
     */
    fun getPreviousSessionId(): String

    companion object {
        /**
         * Constant representing the absence of a session ID.
         */
        const val NO_SESSION_ID = ""

        @JvmStatic
        fun getNoop(): SessionProvider = NO_OP

        private val NO_OP: SessionProvider by lazy {
            object : SessionProvider {
                override fun getSessionId(): String = NO_SESSION_ID

                override fun getPreviousSessionId(): String = NO_SESSION_ID
            }
        }
    }
}
