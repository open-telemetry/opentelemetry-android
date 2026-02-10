/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.EndpointConnectivity
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class GrpcExportConfigurationTest {
    private lateinit var otelConfig: OpenTelemetryConfiguration

    @Before
    fun setUp() {
        otelConfig = OpenTelemetryConfiguration(clock = FakeClock())
    }

    @Test
    fun testDefaults() {
        otelConfig.grpcExport { }
        val config = otelConfig.grpcExportConfig!!

        val expectedHeaders = emptyMap<String, String>()
        val expectedCompression = Compression.GZIP

        config.spansEndpoint().assertEndpointConfig("", expectedHeaders, expectedCompression)
        config.logsEndpoint().assertEndpointConfig("", expectedHeaders, expectedCompression)
        config.metricsEndpoint().assertEndpointConfig("", expectedHeaders, expectedCompression)
        assertEquals("", config.endpoint)
        assertEquals(expectedHeaders, config.headers)
        assertEquals(expectedCompression, config.compression)
    }

    @Test
    fun testEndpointConfiguration() {
        val endpoint = "https://collector.example.com:4317"
        val headers = mapOf("Authorization" to "Bearer token")

        otelConfig.grpcExport {
            this.endpoint = endpoint
            this.headers = headers
        }
        val config = otelConfig.grpcExportConfig!!

        config.spansEndpoint().assertEndpointConfig(endpoint, headers, Compression.GZIP)
        config.logsEndpoint().assertEndpointConfig(endpoint, headers, Compression.GZIP)
        config.metricsEndpoint().assertEndpointConfig(endpoint, headers, Compression.GZIP)
        assertEquals(endpoint, config.endpoint)
        assertEquals(headers, config.headers)
    }

    @Test
    fun testCompressionNone() {
        otelConfig.grpcExport {
            endpoint = "https://collector.example.com:4317"
            compression = Compression.NONE
        }
        val config = otelConfig.grpcExportConfig!!

        config.spansEndpoint().assertEndpointConfig(
            "https://collector.example.com:4317",
            emptyMap(),
            Compression.NONE,
        )
    }

    @Test
    fun testIndividualEndpointOverrides() {
        val baseEndpoint = "https://collector.example.com:4317"
        val baseHeaders = mapOf("base-header" to "base-value")

        val spansEndpoint = "https://spans.collector.example.com:4317"
        val spansHeaders = mapOf("spans-header" to "spans-value")

        val logsEndpoint = "https://logs.collector.example.com:4317"
        val logsHeaders = mapOf("logs-header" to "logs-value")

        val metricsEndpoint = "https://metrics.collector.example.com:4317"
        val metricsHeaders = mapOf("metrics-header" to "metrics-value")

        otelConfig.grpcExport {
            endpoint = baseEndpoint
            headers = baseHeaders

            spans {
                url = spansEndpoint
                this.headers = spansHeaders
                compression = Compression.NONE
            }
            logs {
                url = logsEndpoint
                this.headers = logsHeaders
                compression = Compression.NONE
            }
            metrics {
                url = metricsEndpoint
                this.headers = metricsHeaders
                compression = Compression.NONE
            }
        }
        val config = otelConfig.grpcExportConfig!!

        config.spansEndpoint().assertEndpointConfig(
            spansEndpoint,
            spansHeaders + baseHeaders,
            Compression.NONE,
        )
        config.logsEndpoint().assertEndpointConfig(
            logsEndpoint,
            logsHeaders + baseHeaders,
            Compression.NONE,
        )
        config.metricsEndpoint().assertEndpointConfig(
            metricsEndpoint,
            metricsHeaders + baseHeaders,
            Compression.NONE,
        )
    }

    @Test
    fun testSignalSpecificOverridesFallBackToBase() {
        val baseEndpoint = "https://collector.example.com:4317"
        val baseHeaders = mapOf("base-header" to "base-value")
        val spansHeaders = mapOf("spans-header" to "spans-value")

        otelConfig.grpcExport {
            endpoint = baseEndpoint
            headers = baseHeaders

            spans {
                this.headers = spansHeaders
            }
        }
        val config = otelConfig.grpcExportConfig!!

        config.spansEndpoint().assertEndpointConfig(
            baseEndpoint,
            spansHeaders + baseHeaders,
            Compression.GZIP,
        )
        config.logsEndpoint().assertEndpointConfig(
            baseEndpoint,
            baseHeaders,
            Compression.GZIP,
        )
        config.metricsEndpoint().assertEndpointConfig(
            baseEndpoint,
            baseHeaders,
            Compression.GZIP,
        )
    }

    @Test
    fun testSpansEndpointOverrideOnlyUrl() {
        val baseEndpoint = "https://collector.example.com:4317"
        val spansEndpoint = "https://spans.collector.example.com:4317"

        otelConfig.grpcExport {
            endpoint = baseEndpoint
            spans {
                url = spansEndpoint
            }
        }
        val config = otelConfig.grpcExportConfig!!

        config.spansEndpoint().assertEndpointConfig(
            spansEndpoint,
            emptyMap(),
            Compression.GZIP,
        )
        config.logsEndpoint().assertEndpointConfig(
            baseEndpoint,
            emptyMap(),
            Compression.GZIP,
        )
    }

    @Test
    fun testLogsEndpointOverrideOnlyCompression() {
        val baseEndpoint = "https://collector.example.com:4317"

        otelConfig.grpcExport {
            endpoint = baseEndpoint
            logs {
                compression = Compression.NONE
            }
        }
        val config = otelConfig.grpcExportConfig!!

        config.logsEndpoint().assertEndpointConfig(
            baseEndpoint,
            emptyMap(),
            Compression.NONE,
        )
        config.spansEndpoint().assertEndpointConfig(
            baseEndpoint,
            emptyMap(),
            Compression.GZIP,
        )
    }

    @Test
    fun testMetricsEndpointOverrideOnlyHeaders() {
        val baseEndpoint = "https://collector.example.com:4317"
        val metricsHeaders = mapOf("metrics-header" to "metrics-value")

        otelConfig.grpcExport {
            endpoint = baseEndpoint
            metrics {
                this.headers = metricsHeaders
            }
        }
        val config = otelConfig.grpcExportConfig!!

        config.metricsEndpoint().assertEndpointConfig(
            baseEndpoint,
            metricsHeaders,
            Compression.GZIP,
        )
    }

    @Test
    fun testBlankSignalUrlFallsBackToBase() {
        val baseEndpoint = "https://collector.example.com:4317"

        otelConfig.grpcExport {
            endpoint = baseEndpoint
            spans {
                url = ""
            }
        }
        val config = otelConfig.grpcExportConfig!!

        config.spansEndpoint().assertEndpointConfig(
            baseEndpoint,
            emptyMap(),
            Compression.GZIP,
        )
    }

    @Test
    fun testNullSignalCompressionFallsBackToBase() {
        val baseEndpoint = "https://collector.example.com:4317"

        otelConfig.grpcExport {
            endpoint = baseEndpoint
            compression = Compression.NONE
            spans {
                compression = null
            }
        }
        val config = otelConfig.grpcExportConfig!!

        config.spansEndpoint().assertEndpointConfig(
            baseEndpoint,
            emptyMap(),
            Compression.NONE,
        )
    }

    @Test
    fun testHeadersMergeCorrectly() {
        val baseHeaders = mapOf("base" to "value1")
        val signalHeaders = mapOf("signal" to "value2")

        otelConfig.grpcExport {
            endpoint = "https://collector.example.com:4317"
            headers = baseHeaders
            spans {
                this.headers = signalHeaders
            }
        }
        val config = otelConfig.grpcExportConfig!!

        val spanEndpoint = config.spansEndpoint()
        assertEquals(signalHeaders + baseHeaders, spanEndpoint.getHeaders())
    }

    private fun EndpointConnectivity.assertEndpointConfig(
        expectedUrl: String,
        expectedHeaders: Map<String, String>,
        expectedCompression: Compression,
    ) {
        assertEquals(expectedUrl, getUrl())
        assertEquals(expectedHeaders, getHeaders())
        assertEquals(expectedCompression, getCompression())
    }
}
