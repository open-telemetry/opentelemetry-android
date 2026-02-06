/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.EndpointConnectivity
import io.opentelemetry.android.agent.connectivity.ExportProtocol
import io.opentelemetry.android.agent.connectivity.GrpcEndpointConnectivity
import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

internal class ExportConfigurationTest {
    private lateinit var otelConfig: OpenTelemetryConfiguration

    @Before
    fun setUp() {
        otelConfig = OpenTelemetryConfiguration(clock = FakeClock())
    }

    @Test
    fun testDefaultsToHttp() {
        otelConfig.export { }
        val config = otelConfig.unifiedExportConfig!!

        assertEquals(ExportProtocol.HTTP, config.protocol)
        assertEquals("", config.endpoint)
        assertEquals(emptyMap<String, String>(), config.headers)
        assertEquals(Compression.GZIP, config.compression)
    }

    @Test
    fun testHttpProtocolConfiguration() {
        val endpoint = "https://collector.example.com:4318"
        val headers = mapOf("Authorization" to "Bearer token")

        otelConfig.export {
            protocol = ExportProtocol.HTTP
            this.endpoint = endpoint
            this.headers = headers
        }
        val config = otelConfig.unifiedExportConfig!!

        assertEquals(ExportProtocol.HTTP, config.protocol)
        assertTrue(config.spansEndpoint() is HttpEndpointConnectivity)
        config.spansEndpoint().assertEndpointConfig("$endpoint/v1/traces", headers, Compression.GZIP)
        config.logsEndpoint().assertEndpointConfig("$endpoint/v1/logs", headers, Compression.GZIP)
        config.metricsEndpoint().assertEndpointConfig("$endpoint/v1/metrics", headers, Compression.GZIP)
    }

    @Test
    fun testGrpcProtocolConfiguration() {
        val endpoint = "https://collector.example.com:4317"
        val headers = mapOf("Authorization" to "Bearer token")

        otelConfig.export {
            protocol = ExportProtocol.GRPC
            this.endpoint = endpoint
            this.headers = headers
        }
        val config = otelConfig.unifiedExportConfig!!

        assertEquals(ExportProtocol.GRPC, config.protocol)
        assertTrue(config.spansEndpoint() is GrpcEndpointConnectivity)
        config.spansEndpoint().assertEndpointConfig(endpoint, headers, Compression.GZIP)
        config.logsEndpoint().assertEndpointConfig(endpoint, headers, Compression.GZIP)
        config.metricsEndpoint().assertEndpointConfig(endpoint, headers, Compression.GZIP)
    }

    @Test
    fun testHttpSpansEndpointWithOverride() {
        val baseEndpoint = "https://collector.example.com:4318"
        val spansEndpoint = "https://spans.collector.example.com:4318"
        val spansHeaders = mapOf("spans-header" to "spans-value")

        otelConfig.export {
            protocol = ExportProtocol.HTTP
            endpoint = baseEndpoint
            spans {
                url = spansEndpoint
                this.headers = spansHeaders
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        assertTrue(config.spansEndpoint() is HttpEndpointConnectivity)
        config.spansEndpoint().assertEndpointConfig("$spansEndpoint/v1/traces", spansHeaders, Compression.GZIP)
    }

    @Test
    fun testHttpLogsEndpointWithOverride() {
        val baseEndpoint = "https://collector.example.com:4318"
        val logsEndpoint = "https://logs.collector.example.com:4318"
        val logsHeaders = mapOf("logs-header" to "logs-value")

        otelConfig.export {
            protocol = ExportProtocol.HTTP
            endpoint = baseEndpoint
            logs {
                url = logsEndpoint
                this.headers = logsHeaders
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        assertTrue(config.logsEndpoint() is HttpEndpointConnectivity)
        config.logsEndpoint().assertEndpointConfig("$logsEndpoint/v1/logs", logsHeaders, Compression.GZIP)
    }

    @Test
    fun testHttpMetricsEndpointWithOverride() {
        val baseEndpoint = "https://collector.example.com:4318"
        val metricsEndpoint = "https://metrics.collector.example.com:4318"
        val metricsHeaders = mapOf("metrics-header" to "metrics-value")

        otelConfig.export {
            protocol = ExportProtocol.HTTP
            endpoint = baseEndpoint
            metrics {
                url = metricsEndpoint
                this.headers = metricsHeaders
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        assertTrue(config.metricsEndpoint() is HttpEndpointConnectivity)
        config.metricsEndpoint().assertEndpointConfig("$metricsEndpoint/v1/metrics", metricsHeaders, Compression.GZIP)
    }

    @Test
    fun testGrpcSpansEndpointWithOverride() {
        val baseEndpoint = "https://collector.example.com:4317"
        val spansEndpoint = "https://spans.collector.example.com:4317"

        otelConfig.export {
            protocol = ExportProtocol.GRPC
            endpoint = baseEndpoint
            spans {
                url = spansEndpoint
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        assertTrue(config.spansEndpoint() is GrpcEndpointConnectivity)
        config.spansEndpoint().assertEndpointConfig(spansEndpoint, emptyMap(), Compression.GZIP)
    }

    @Test
    fun testGrpcLogsEndpointWithOverride() {
        val baseEndpoint = "https://collector.example.com:4317"
        val logsEndpoint = "https://logs.collector.example.com:4317"

        otelConfig.export {
            protocol = ExportProtocol.GRPC
            endpoint = baseEndpoint
            logs {
                url = logsEndpoint
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        assertTrue(config.logsEndpoint() is GrpcEndpointConnectivity)
        config.logsEndpoint().assertEndpointConfig(logsEndpoint, emptyMap(), Compression.GZIP)
    }

    @Test
    fun testGrpcMetricsEndpointWithOverride() {
        val baseEndpoint = "https://collector.example.com:4317"
        val metricsEndpoint = "https://metrics.collector.example.com:4317"

        otelConfig.export {
            protocol = ExportProtocol.GRPC
            endpoint = baseEndpoint
            metrics {
                url = metricsEndpoint
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        assertTrue(config.metricsEndpoint() is GrpcEndpointConnectivity)
        config.metricsEndpoint().assertEndpointConfig(metricsEndpoint, emptyMap(), Compression.GZIP)
    }

    @Test
    fun testCompressionOverrideForHttp() {
        otelConfig.export {
            protocol = ExportProtocol.HTTP
            endpoint = "https://collector.example.com:4318"
            compression = Compression.NONE
        }
        val config = otelConfig.unifiedExportConfig!!

        config.spansEndpoint().assertEndpointConfig(
            "https://collector.example.com:4318/v1/traces",
            emptyMap(),
            Compression.NONE,
        )
    }

    @Test
    fun testCompressionOverrideForGrpc() {
        otelConfig.export {
            protocol = ExportProtocol.GRPC
            endpoint = "https://collector.example.com:4317"
            compression = Compression.NONE
        }
        val config = otelConfig.unifiedExportConfig!!

        config.spansEndpoint().assertEndpointConfig(
            "https://collector.example.com:4317",
            emptyMap(),
            Compression.NONE,
        )
    }

    @Test
    fun testSignalSpecificCompressionOverride() {
        otelConfig.export {
            protocol = ExportProtocol.GRPC
            endpoint = "https://collector.example.com:4317"
            compression = Compression.GZIP
            spans {
                compression = Compression.NONE
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        config.spansEndpoint().assertEndpointConfig(
            "https://collector.example.com:4317",
            emptyMap(),
            Compression.NONE,
        )
        config.logsEndpoint().assertEndpointConfig(
            "https://collector.example.com:4317",
            emptyMap(),
            Compression.GZIP,
        )
    }

    @Test
    fun testBlankSignalEndpointFallsBackToBase() {
        val baseEndpoint = "https://collector.example.com:4317"

        otelConfig.export {
            protocol = ExportProtocol.GRPC
            endpoint = baseEndpoint
            spans {
                url = ""
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        config.spansEndpoint().assertEndpointConfig(baseEndpoint, emptyMap(), Compression.GZIP)
    }

    @Test
    fun testNullSignalCompressionFallsBackToBase() {
        otelConfig.export {
            protocol = ExportProtocol.GRPC
            endpoint = "https://collector.example.com:4317"
            compression = Compression.NONE
            logs {
                compression = null
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        config.logsEndpoint().assertEndpointConfig(
            "https://collector.example.com:4317",
            emptyMap(),
            Compression.NONE,
        )
    }

    @Test
    fun testHeadersMergeCorrectlyForHttp() {
        val baseHeaders = mapOf("base" to "value1")
        val signalHeaders = mapOf("signal" to "value2")

        otelConfig.export {
            protocol = ExportProtocol.HTTP
            endpoint = "https://collector.example.com:4318"
            headers = baseHeaders
            spans {
                this.headers = signalHeaders
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        val spanEndpoint = config.spansEndpoint()
        assertEquals(signalHeaders + baseHeaders, spanEndpoint.getHeaders())
    }

    @Test
    fun testHeadersMergeCorrectlyForGrpc() {
        val baseHeaders = mapOf("base" to "value1")
        val signalHeaders = mapOf("signal" to "value2")

        otelConfig.export {
            protocol = ExportProtocol.GRPC
            endpoint = "https://collector.example.com:4317"
            headers = baseHeaders
            metrics {
                this.headers = signalHeaders
            }
        }
        val config = otelConfig.unifiedExportConfig!!

        val metricsEndpoint = config.metricsEndpoint()
        assertEquals(signalHeaders + baseHeaders, metricsEndpoint.getHeaders())
    }

    @Test
    fun testAllSignalsWithDifferentProtocol() {
        otelConfig.export {
            protocol = ExportProtocol.GRPC
            endpoint = "https://collector.example.com:4317"

            spans { url = "https://spans:4317" }
            logs { url = "https://logs:4317" }
            metrics { url = "https://metrics:4317" }
        }
        val config = otelConfig.unifiedExportConfig!!

        assertTrue(config.spansEndpoint() is GrpcEndpointConnectivity)
        assertTrue(config.logsEndpoint() is GrpcEndpointConnectivity)
        assertTrue(config.metricsEndpoint() is GrpcEndpointConnectivity)

        assertEquals("https://spans:4317", config.spansEndpoint().getUrl())
        assertEquals("https://logs:4317", config.logsEndpoint().getUrl())
        assertEquals("https://metrics:4317", config.metricsEndpoint().getUrl())
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
