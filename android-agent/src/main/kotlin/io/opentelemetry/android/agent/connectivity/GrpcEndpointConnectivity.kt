/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

internal class GrpcEndpointConnectivity private constructor(
    private val endpoint: String,
    private val headers: Map<String, String>,
    private val compression: Compression,
) : EndpointConnectivity {
    companion object {
        fun create(
            endpoint: String,
            headers: Map<String, String>,
            compression: Compression,
        ): GrpcEndpointConnectivity = GrpcEndpointConnectivity(endpoint, headers, compression)
    }

    override fun getUrl(): String = endpoint

    override fun getHeaders(): Map<String, String> = headers

    override fun getCompression(): Compression = compression
}
