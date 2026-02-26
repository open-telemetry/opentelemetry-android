/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

internal class HttpEndpointConnectivity private constructor(
    private val url: String,
    private val headers: Map<String, String>,
    private val compression: Compression,
    private val sslContext: SSLContextConnectivity?,
) : EndpointConnectivity {
    companion object {
        fun forTraces(
            baseUrl: String,
            headers: Map<String, String>,
            compression: Compression,
            sslContext: SSLContextConnectivity?,
        ): HttpEndpointConnectivity = HttpEndpointConnectivity(baseUrl.trimEnd('/') + "/v1/traces", headers, compression, sslContext)

        fun forLogs(
            baseUrl: String,
            headers: Map<String, String>,
            compression: Compression,
            sslContext: SSLContextConnectivity?,
        ): HttpEndpointConnectivity = HttpEndpointConnectivity(baseUrl.trimEnd('/') + "/v1/logs", headers, compression, sslContext)

        fun forMetrics(
            baseUrl: String,
            headers: Map<String, String>,
            compression: Compression,
            sslContext: SSLContextConnectivity?,
        ): HttpEndpointConnectivity = HttpEndpointConnectivity(baseUrl.trimEnd('/') + "/v1/metrics", headers, compression, sslContext)
    }

    override fun getUrl(): String = url

    override fun getHeaders(): Map<String, String> = headers

    override fun getCompression(): Compression = compression

    override fun getSslContext(): SSLContextConnectivity? = sslContext
}
