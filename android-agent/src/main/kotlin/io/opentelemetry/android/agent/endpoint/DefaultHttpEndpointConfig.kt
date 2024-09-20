/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.endpoint

internal data class DefaultHttpEndpointConfig(
    private val baseUrl: String,
    private val headers: Map<String, String>,
) : EndpointConfig {
    override fun getSpanExporterUrl(): String {
        return "$baseUrl/v1/traces"
    }

    override fun getLogRecordExporterUrl(): String {
        return "$baseUrl/v1/logs"
    }

    override fun getHeaders(): Map<String, String> {
        return headers
    }
}
