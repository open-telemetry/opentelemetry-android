/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.ExportProtocol
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull

internal class OpenTelemetryConfigurationExportTest {
    private lateinit var config: OpenTelemetryConfiguration

    @Before
    fun setUp() {
        config = OpenTelemetryConfiguration(clock = FakeClock())
    }

    @Test
    fun testHttpExportDefaultConfig() {
        assertNotNull(config.exportConfig)
        assertEquals("", config.exportConfig.baseUrl)
        assertEquals(emptyMap<String, String>(), config.exportConfig.baseHeaders)
        assertEquals(Compression.GZIP, config.exportConfig.compression)
    }

    @Test
    fun testGrpcExportConfigIsNullByDefault() {
        assertNull(config.grpcExportConfig)
    }

    @Test
    fun testUnifiedExportConfigIsNullByDefault() {
        assertNull(config.unifiedExportConfig)
    }

    @Test
    fun testHttpExportDslSetsConfig() {
        config.httpExport {
            baseUrl = "https://http.collector.example.com:4318"
            baseHeaders = mapOf("Auth" to "token")
            compression = Compression.NONE
        }

        assertEquals("https://http.collector.example.com:4318", config.exportConfig.baseUrl)
        assertEquals(mapOf("Auth" to "token"), config.exportConfig.baseHeaders)
        assertEquals(Compression.NONE, config.exportConfig.compression)
    }

    @Test
    fun testGrpcExportDslCreatesConfig() {
        config.grpcExport {
            endpoint = "https://grpc.collector.example.com:4317"
            headers = mapOf("Auth" to "token")
            compression = Compression.GZIP
        }

        assertNotNull(config.grpcExportConfig)
        assertEquals("https://grpc.collector.example.com:4317", config.grpcExportConfig!!.endpoint)
        assertEquals(mapOf("Auth" to "token"), config.grpcExportConfig!!.headers)
        assertEquals(Compression.GZIP, config.grpcExportConfig!!.compression)
    }

    @Test
    fun testUnifiedExportDslCreatesConfig() {
        config.export {
            protocol = ExportProtocol.GRPC
            endpoint = "https://collector.example.com:4317"
            headers = mapOf("Auth" to "token")
            compression = Compression.NONE
        }

        assertNotNull(config.unifiedExportConfig)
        assertEquals(ExportProtocol.GRPC, config.unifiedExportConfig!!.protocol)
        assertEquals("https://collector.example.com:4317", config.unifiedExportConfig!!.endpoint)
        assertEquals(mapOf("Auth" to "token"), config.unifiedExportConfig!!.headers)
        assertEquals(Compression.NONE, config.unifiedExportConfig!!.compression)
    }

    @Test
    fun testUnifiedExportDefaultsToHttp() {
        config.export { }

        assertNotNull(config.unifiedExportConfig)
        assertEquals(ExportProtocol.HTTP, config.unifiedExportConfig!!.protocol)
    }

    @Test
    fun testMultipleExportConfigsCanCoexist() {
        config.httpExport {
            baseUrl = "https://http.collector.example.com:4318"
        }
        config.grpcExport {
            endpoint = "https://grpc.collector.example.com:4317"
        }
        config.export {
            protocol = ExportProtocol.GRPC
            endpoint = "https://unified.collector.example.com:4317"
        }

        assertNotNull(config.exportConfig)
        assertNotNull(config.grpcExportConfig)
        assertNotNull(config.unifiedExportConfig)
        assertEquals("https://http.collector.example.com:4318", config.exportConfig.baseUrl)
        assertEquals("https://grpc.collector.example.com:4317", config.grpcExportConfig!!.endpoint)
        assertEquals("https://unified.collector.example.com:4317", config.unifiedExportConfig!!.endpoint)
    }

    @Test
    fun testGrpcExportWithSignalOverrides() {
        config.grpcExport {
            endpoint = "https://collector.example.com:4317"
            spans {
                url = "https://spans.collector.example.com:4317"
            }
        }

        val spansEndpoint = config.grpcExportConfig!!.spansEndpoint()
        assertEquals("https://spans.collector.example.com:4317", spansEndpoint.getUrl())
    }

    @Test
    fun testUnifiedExportWithSignalOverrides() {
        config.export {
            protocol = ExportProtocol.GRPC
            endpoint = "https://collector.example.com:4317"
            logs {
                url = "https://logs.collector.example.com:4317"
            }
        }

        val logsEndpoint = config.unifiedExportConfig!!.logsEndpoint()
        assertEquals("https://logs.collector.example.com:4317", logsEndpoint.getUrl())
    }
}
