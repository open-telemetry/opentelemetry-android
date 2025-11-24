/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class HttpExportConfigurationTest {
    @Test
    fun testDefaults() {
        val otelConfig = OpenTelemetryConfiguration()
        val config = otelConfig.exportConfig
        val expectedHeaders = emptyMap<String, String>()
        val expectedCompression = Compression.GZIP
        config
            .spansEndpoint()
            .assertEndpointConfig("/v1/traces", expectedHeaders, expectedCompression)
        config.logsEndpoint().assertEndpointConfig("/v1/logs", expectedHeaders, expectedCompression)
        config
            .metricsEndpoint()
            .assertEndpointConfig("/v1/metrics", expectedHeaders, expectedCompression)
        assertEquals("", config.baseUrl)
        assertEquals(expectedHeaders, config.baseHeaders)
        assertEquals(expectedCompression, config.compression)
    }

    @Test
    fun testBaseValueOverride() {
        val otelConfig = OpenTelemetryConfiguration()
        val url = "http://localhost:4318/"
        val headers = mapOf("my-header" to "my-value")
        val config =
            otelConfig.exportConfig.apply {
                baseUrl = url
                baseHeaders = headers
            }

        config.spansEndpoint().assertEndpointConfig("${url}v1/traces", headers, Compression.GZIP)
        config.logsEndpoint().assertEndpointConfig("${url}v1/logs", headers, Compression.GZIP)
        config.metricsEndpoint().assertEndpointConfig("${url}v1/metrics", headers, Compression.GZIP)
        assertEquals(url, config.baseUrl)
        assertEquals(headers, config.baseHeaders)
    }

    @Test
    fun testIndividualEndpointOverrides() {
        val otelConfig = OpenTelemetryConfiguration()
        val baseUrl = "http://localhost:4318/"
        val baseHeaders = mapOf("my-header" to "my-value")

        val spanUrl = "http://localhost:4318/spans/"
        val spanHeaders = mapOf("span-header" to "span-value")

        val logUrl = "http://localhost:4318/logs/"
        val logHeaders = mapOf("log-header" to "log-value")

        val metricsUrl = "http://localhost:4318/metrics/"
        val metricsHeaders = mapOf("metrics-header" to "metrics-value")

        val expectedCompression = Compression.NONE

        val config =
            otelConfig.exportConfig.apply {
                this.baseUrl = baseUrl
                this.baseHeaders = baseHeaders

                spans {
                    url = spanUrl
                    headers = spanHeaders
                    compression = expectedCompression
                }
                logs {
                    url = logUrl
                    headers = logHeaders
                    compression = expectedCompression
                }
                metrics {
                    url = metricsUrl
                    headers = metricsHeaders
                    compression = expectedCompression
                }
            }

        config
            .spansEndpoint()
            .assertEndpointConfig(
                "${spanUrl}v1/traces",
                spanHeaders + baseHeaders,
                expectedCompression,
            )
        config
            .logsEndpoint()
            .assertEndpointConfig("${logUrl}v1/logs", logHeaders + baseHeaders, expectedCompression)
        config.metricsEndpoint().assertEndpointConfig(
            "${metricsUrl}v1/metrics",
            metricsHeaders + baseHeaders,
            expectedCompression,
        )
        assertEquals(baseUrl, config.baseUrl)
        assertEquals(baseHeaders, config.baseHeaders)
    }

    @Test
    fun testIndividualEndpointOverrides2() {
        val otelConfig = OpenTelemetryConfiguration()
        val baseUrl = "http://localhost:4318/"
        val baseHeaders = mapOf("my-header" to "my-value")

        val spanUrl = "http://localhost:4318/spans/"
        val spanHeaders = mapOf("span-header" to "span-value")

        val logUrl = "http://localhost:4318/logs/"
        val logHeaders = mapOf("log-header" to "log-value")

        val metricsUrl = "http://localhost:4318/metrics/"
        val metricsHeaders = mapOf("metrics-header" to "metrics-value")

        val expectedCompression = Compression.GZIP

        val config =
            otelConfig.exportConfig.apply {
                spans {
                    url = spanUrl
                    headers = spanHeaders
                }
                logs {
                    url = logUrl
                    headers = logHeaders
                }
                metrics {
                    url = metricsUrl
                    headers = metricsHeaders
                }

                // altering base values after setting individual endpoints should give same result as
                // when setting base values before.
                this.baseUrl = baseUrl
                this.baseHeaders = baseHeaders
            }

        config
            .spansEndpoint()
            .assertEndpointConfig(
                "${spanUrl}v1/traces",
                spanHeaders + baseHeaders,
                expectedCompression,
            )
        config
            .logsEndpoint()
            .assertEndpointConfig("${logUrl}v1/logs", logHeaders + baseHeaders, expectedCompression)
        config.metricsEndpoint().assertEndpointConfig(
            "${metricsUrl}v1/metrics",
            metricsHeaders + baseHeaders,
            expectedCompression,
        )
        assertEquals(baseUrl, config.baseUrl)
        assertEquals(baseHeaders, config.baseHeaders)
    }

    private fun HttpEndpointConnectivity.assertEndpointConfig(
        expectedUrl: String,
        expectedHeaders: Map<String, String>,
        expectedCompression: Compression,
    ) {
        assertEquals(expectedUrl, getUrl())
        assertEquals(expectedHeaders, getHeaders())
        assertEquals(expectedCompression, getCompression())
    }
}
