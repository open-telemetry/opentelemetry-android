/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HttpEndpointConnectivityTest {
    @Test
    fun `Validate exporter endpoint configuration`() {
        val headers = mapOf("Authorization" to "Basic something")
        val compression = Compression.NONE
        val sslContext = SSLContextConnectivity(mockk(), mockk())
        val clientTls: ClientTlsConnectivity = mockk()
        val tracesConnectivity =
            HttpEndpointConnectivity.forTraces("http://some.endpoint", headers, compression, sslContext, clientTls)
        val logsConnectivity =
            HttpEndpointConnectivity.forLogs("http://some.endpoint/", headers, compression, sslContext, clientTls)
        val metricsConnectivity =
            HttpEndpointConnectivity.forMetrics("http://some.endpoint", headers, compression, sslContext, clientTls)

        assertThat(tracesConnectivity.getUrl()).isEqualTo("http://some.endpoint/v1/traces")
        assertThat(tracesConnectivity.getHeaders()).isEqualTo(headers)
        assertThat(tracesConnectivity.getCompression()).isEqualTo(Compression.NONE)
        assertThat(tracesConnectivity.getSslContext()).isEqualTo(sslContext)
        assertThat(tracesConnectivity.getClientTls()).isEqualTo(clientTls)
        assertThat(logsConnectivity.getUrl()).isEqualTo("http://some.endpoint/v1/logs")
        assertThat(logsConnectivity.getHeaders()).isEqualTo(headers)
        assertThat(logsConnectivity.getCompression()).isEqualTo(Compression.NONE)
        assertThat(logsConnectivity.getSslContext()).isEqualTo(sslContext)
        assertThat(logsConnectivity.getClientTls()).isEqualTo(clientTls)
        assertThat(metricsConnectivity.getUrl()).isEqualTo("http://some.endpoint/v1/metrics")
        assertThat(metricsConnectivity.getHeaders()).isEqualTo(headers)
        assertThat(metricsConnectivity.getCompression()).isEqualTo(Compression.NONE)
        assertThat(metricsConnectivity.getSslContext()).isEqualTo(sslContext)
        assertThat(metricsConnectivity.getClientTls()).isEqualTo(clientTls)
    }
}
