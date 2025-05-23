/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

interface SessionProvider {
    fun getSessionId(): String

    companion object {
        @JvmStatic
        fun getNoop(): SessionProvider = NO_OP

        private val NO_OP: SessionProvider by lazy {
            object : SessionProvider {
                override fun getSessionId(): String = ""
            }
        }
    }
}
