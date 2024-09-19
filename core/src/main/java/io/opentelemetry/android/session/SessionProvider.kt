/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

interface SessionProvider {
    companion object {
        @JvmStatic
        val noop: SessionProvider =
            object : SessionProvider {
                override fun getSessionId(): String? {
                    return null
                }
            }
    }

    fun getSessionId(): String?
}
