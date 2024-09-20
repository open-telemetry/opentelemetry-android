/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.endpoint

interface EndpointConfig {
    fun getUrl(): String

    fun getHeaders(): Map<String, String>

    companion object {
        fun getDefault(
            url: String,
            headers: Map<String, String> = emptyMap(),
        ): EndpointConfig {
            return DefaultEndpointConfig(url, headers)
        }
    }
}
