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
