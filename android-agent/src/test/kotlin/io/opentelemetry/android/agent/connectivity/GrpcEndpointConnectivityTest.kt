/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GrpcEndpointConnectivityTest {
    @Test
    fun `create returns connectivity with provided endpoint`() {
        val endpoint = "https://collector.example.com:4317"
        val headers = mapOf("Authorization" to "Bearer token")
        val compression = Compression.GZIP

        val connectivity = GrpcEndpointConnectivity.create(endpoint, headers, compression)

        assertThat(connectivity.getUrl()).isEqualTo(endpoint)
    }

    @Test
    fun `create returns connectivity with provided headers`() {
        val endpoint = "https://collector.example.com:4317"
        val headers = mapOf("Authorization" to "Bearer token", "X-Custom" to "value")
        val compression = Compression.GZIP

        val connectivity = GrpcEndpointConnectivity.create(endpoint, headers, compression)

        assertThat(connectivity.getHeaders()).isEqualTo(headers)
        assertThat(connectivity.getHeaders()).containsEntry("Authorization", "Bearer token")
        assertThat(connectivity.getHeaders()).containsEntry("X-Custom", "value")
    }

    @Test
    fun `create with GZIP compression returns GZIP`() {
        val endpoint = "https://collector.example.com:4317"
        val headers = emptyMap<String, String>()
        val compression = Compression.GZIP

        val connectivity = GrpcEndpointConnectivity.create(endpoint, headers, compression)

        assertThat(connectivity.getCompression()).isEqualTo(Compression.GZIP)
    }

    @Test
    fun `create with NONE compression returns NONE`() {
        val endpoint = "https://collector.example.com:4317"
        val headers = emptyMap<String, String>()
        val compression = Compression.NONE

        val connectivity = GrpcEndpointConnectivity.create(endpoint, headers, compression)

        assertThat(connectivity.getCompression()).isEqualTo(Compression.NONE)
    }

    @Test
    fun `create with empty headers returns empty map`() {
        val endpoint = "https://collector.example.com:4317"
        val headers = emptyMap<String, String>()
        val compression = Compression.GZIP

        val connectivity = GrpcEndpointConnectivity.create(endpoint, headers, compression)

        assertThat(connectivity.getHeaders()).isEmpty()
    }

    @Test
    fun `create with empty endpoint returns empty string`() {
        val endpoint = ""
        val headers = emptyMap<String, String>()
        val compression = Compression.GZIP

        val connectivity = GrpcEndpointConnectivity.create(endpoint, headers, compression)

        assertThat(connectivity.getUrl()).isEmpty()
    }

    @Test
    fun `getUrl returns endpoint unchanged without path modification`() {
        val endpoint = "https://collector.example.com:4317/custom/path"
        val connectivity = GrpcEndpointConnectivity.create(endpoint, emptyMap(), Compression.GZIP)

        assertThat(connectivity.getUrl()).isEqualTo(endpoint)
    }
}
