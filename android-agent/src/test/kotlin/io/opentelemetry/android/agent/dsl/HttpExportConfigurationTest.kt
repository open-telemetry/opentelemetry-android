/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.mockk
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity
import io.opentelemetry.android.agent.connectivity.SSLContextConnectivity
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class HttpExportConfigurationTest {
    private lateinit var otelConfig: OpenTelemetryConfiguration

    @Before
    fun setUp() {
        otelConfig = OpenTelemetryConfiguration(clock = FakeClock())
    }

    @Test
    fun testDefaults() {
        val config = otelConfig.exportConfig
        val expectedHeaders = emptyMap<String, String>()
        val expectedCompression = Compression.GZIP
        val expectedSslContext: SSLContextConnectivity? = null
        config
            .spansEndpoint()
            .assertEndpointConfig("/v1/traces", expectedHeaders, expectedCompression, expectedSslContext)
        config.logsEndpoint().assertEndpointConfig("/v1/logs", expectedHeaders, expectedCompression, expectedSslContext)
        config
            .metricsEndpoint()
            .assertEndpointConfig("/v1/metrics", expectedHeaders, expectedCompression, expectedSslContext)
        assertEquals("", config.baseUrl)
        assertEquals(expectedHeaders, config.baseHeaders)
        assertEquals(expectedCompression, config.compression)
        assertEquals(expectedSslContext, config.sslContext)
    }

    @Test
    fun testBaseValueOverride() {
        val url = "http://localhost:4318/"
        val headers = mapOf("my-header" to "my-value")
        val config =
            otelConfig.exportConfig.apply {
                baseUrl = url
                baseHeaders = headers
            }

        config.spansEndpoint().assertEndpointConfig("${url}v1/traces", headers, Compression.GZIP, null)
        config.logsEndpoint().assertEndpointConfig("${url}v1/logs", headers, Compression.GZIP, null)
        config.metricsEndpoint().assertEndpointConfig("${url}v1/metrics", headers, Compression.GZIP, null)
        assertEquals(url, config.baseUrl)
        assertEquals(headers, config.baseHeaders)
    }

    @Test
    fun testIndividualEndpointOverrides() {
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
                null,
            )
        config
            .logsEndpoint()
            .assertEndpointConfig("${logUrl}v1/logs", logHeaders + baseHeaders, expectedCompression, null)
        config.metricsEndpoint().assertEndpointConfig(
            "${metricsUrl}v1/metrics",
            metricsHeaders + baseHeaders,
            expectedCompression,
            null,
        )
        assertEquals(baseUrl, config.baseUrl)
        assertEquals(baseHeaders, config.baseHeaders)
    }

    @Test
    fun testIndividualEndpointOverrides2() {
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
                null,
            )
        config
            .logsEndpoint()
            .assertEndpointConfig("${logUrl}v1/logs", logHeaders + baseHeaders, expectedCompression, null)
        config.metricsEndpoint().assertEndpointConfig(
            "${metricsUrl}v1/metrics",
            metricsHeaders + baseHeaders,
            expectedCompression,
            null,
        )
        assertEquals(baseUrl, config.baseUrl)
        assertEquals(baseHeaders, config.baseHeaders)
    }

    @Test
    fun testSslContext() {
        val url = "http://localhost:4318/"
        val headers = mapOf("my-header" to "my-value")
        val expectedSslContext = SSLContextConnectivity(mockk(), mockk())
        val config =
            otelConfig.exportConfig.apply {
                baseUrl = url
                baseHeaders = headers
                sslContext = expectedSslContext
            }

        config.spansEndpoint().assertEndpointConfig("${url}v1/traces", headers, Compression.GZIP, expectedSslContext)
        config.logsEndpoint().assertEndpointConfig("${url}v1/logs", headers, Compression.GZIP, expectedSslContext)
        config.metricsEndpoint().assertEndpointConfig("${url}v1/metrics", headers, Compression.GZIP, expectedSslContext)
    }

    private fun HttpEndpointConnectivity.assertEndpointConfig(
        expectedUrl: String,
        expectedHeaders: Map<String, String>,
        expectedCompression: Compression,
        expectedSslContext: SSLContextConnectivity?,
    ) {
        assertEquals(expectedUrl, getUrl())
        assertEquals(expectedHeaders, getHeaders())
        assertEquals(expectedCompression, getCompression())
        assertEquals(expectedSslContext, getSslContext())
    }
}
