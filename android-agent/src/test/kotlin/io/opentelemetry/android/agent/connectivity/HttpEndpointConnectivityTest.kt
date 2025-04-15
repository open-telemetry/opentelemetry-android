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
        val tracesConnectivity = HttpEndpointConnectivity.forTraces("http://some.endpoint", headers)
        val logsConnectivity = HttpEndpointConnectivity.forLogs("http://some.endpoint/", headers)
        val metricsConnectivity =
            HttpEndpointConnectivity.forMetrics("http://some.endpoint", headers)

        assertThat(tracesConnectivity.getUrl()).isEqualTo("http://some.endpoint/v1/traces")
        assertThat(tracesConnectivity.getHeaders()).isEqualTo(headers)
        assertThat(logsConnectivity.getUrl()).isEqualTo("http://some.endpoint/v1/logs")
        assertThat(logsConnectivity.getHeaders()).isEqualTo(headers)
        assertThat(metricsConnectivity.getUrl()).isEqualTo("http://some.endpoint/v1/metrics")
        assertThat(metricsConnectivity.getHeaders()).isEqualTo(headers)
    }
}
