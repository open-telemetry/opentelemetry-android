/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

internal class HttpEndpointConnectivity private constructor(
    private val url: String,
    private val headers: Map<String, String>,
    private val compression: Compression,
) : EndpointConnectivity {
    companion object {
        fun forTraces(
            baseUrl: String,
            headers: Map<String, String>,
            compression: Compression,
        ): HttpEndpointConnectivity = HttpEndpointConnectivity(baseUrl.trimEnd('/') + "/v1/traces", headers, compression)

        fun forLogs(
            baseUrl: String,
            headers: Map<String, String>,
            compression: Compression,
        ): HttpEndpointConnectivity = HttpEndpointConnectivity(baseUrl.trimEnd('/') + "/v1/logs", headers, compression)

        fun forMetrics(
            baseUrl: String,
            headers: Map<String, String>,
            compression: Compression,
        ): HttpEndpointConnectivity = HttpEndpointConnectivity(baseUrl.trimEnd('/') + "/v1/metrics", headers, compression)
    }

    override fun getUrl(): String = url

    override fun getHeaders(): Map<String, String> = headers

    override fun getCompression(): Compression = compression
}
