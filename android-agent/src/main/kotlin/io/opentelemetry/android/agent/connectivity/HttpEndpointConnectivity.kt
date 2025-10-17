/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

internal class HttpEndpointConnectivity private constructor(
    private val url: String,
    private val headers: Map<String, String>,
) : EndpointConnectivity {
    companion object {
        fun forTraces(
            baseUrl: String,
            headers: Map<String, String> = emptyMap(),
        ): HttpEndpointConnectivity = HttpEndpointConnectivity(baseUrl.trimEnd('/') + "/v1/traces", headers)

        fun forLogs(
            baseUrl: String,
            headers: Map<String, String> = emptyMap(),
        ): HttpEndpointConnectivity = HttpEndpointConnectivity(baseUrl.trimEnd('/') + "/v1/logs", headers)

        fun forMetrics(
            baseUrl: String,
            headers: Map<String, String> = emptyMap(),
        ): HttpEndpointConnectivity = HttpEndpointConnectivity(baseUrl.trimEnd('/') + "/v1/metrics", headers)
    }

    override fun getUrl(): String = url

    override fun getHeaders(): Map<String, String> = headers
}
