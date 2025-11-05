/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HttpEndpointConnectivityTest {
    @Test
    fun `Validate exporter endpoint configuration`() {
        val headers = mapOf("Authorization" to "Basic something")
        val compression = Compression.NONE
        val tracesConnectivity =
            HttpEndpointConnectivity.forTraces("http://some.endpoint", headers, compression)
        val logsConnectivity =
            HttpEndpointConnectivity.forLogs("http://some.endpoint/", headers, compression)
        val metricsConnectivity =
            HttpEndpointConnectivity.forMetrics("http://some.endpoint", headers, compression)

        assertThat(tracesConnectivity.getUrl()).isEqualTo("http://some.endpoint/v1/traces")
        assertThat(tracesConnectivity.getHeaders()).isEqualTo(headers)
        assertThat(tracesConnectivity.getCompression()).isEqualTo(Compression.NONE)
        assertThat(logsConnectivity.getUrl()).isEqualTo("http://some.endpoint/v1/logs")
        assertThat(logsConnectivity.getHeaders()).isEqualTo(headers)
        assertThat(logsConnectivity.getCompression()).isEqualTo(Compression.NONE)
        assertThat(metricsConnectivity.getUrl()).isEqualTo("http://some.endpoint/v1/metrics")
        assertThat(metricsConnectivity.getHeaders()).isEqualTo(headers)
        assertThat(metricsConnectivity.getCompression()).isEqualTo(Compression.NONE)
    }
}
