/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.endpoint

interface EndpointConfig {
    fun getSpanExporterUrl(): String

    fun getLogRecordExporterUrl(): String

    fun getHeaders(): Map<String, String>

    companion object {
        @JvmStatic
        fun getDefault(
            baseUrl: String,
            headers: Map<String, String> = emptyMap(),
        ): EndpointConfig {
            return DefaultHttpEndpointConfig(baseUrl.trimEnd('/'), headers)
        }
    }
}
