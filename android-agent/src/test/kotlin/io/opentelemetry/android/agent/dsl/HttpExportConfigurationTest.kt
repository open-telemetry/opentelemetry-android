/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.mockk
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.agent.connectivity.ClientTlsConnectivity
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
        otelConfig = OpenTelemetryConfiguration(
            clock = FakeClock(),
            instrumentationLoader = FakeInstrumentationLoader()
        )
    }

    @Test
    fun testDefaults() {
        val config = otelConfig.exportConfig
        val expectedHeaders = emptyMap<String, String>()
        val expectedCompression = Compression.GZIP
        val expectedSslContext: SSLContextConnectivity? = null
        val expectedClientTls: ClientTlsConnectivity? = null
        config
            .spansEndpoint()
            .assertEndpointConfig(
                "/v1/traces",
                expectedHeaders,
                expectedCompression,
                expectedSslContext,
                expectedClientTls
            )
        config.logsEndpoint().assertEndpointConfig(
            "/v1/logs",
            expectedHeaders,
            expectedCompression,
            expectedSslContext,
            expectedClientTls
        )
        config
            .metricsEndpoint()
            .assertEndpointConfig(
                "/v1/metrics",
                expectedHeaders,
                expectedCompression,
                expectedSslContext,
                expectedClientTls
            )
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

        config.spansEndpoint()
            .assertEndpointConfig("${url}v1/traces", headers, Compression.GZIP, null, null)
        config.logsEndpoint().assertEndpointConfig("${url}v1/logs", headers, Compression.GZIP, null, null)
        config.metricsEndpoint()
            .assertEndpointConfig("${url}v1/metrics", headers, Compression.GZIP, null, null)
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
                null
            )
        config
            .logsEndpoint()
            .assertEndpointConfig(
                "${logUrl}v1/logs",
                logHeaders + baseHeaders,
                expectedCompression,
                null
            )
        config.metricsEndpoint().assertEndpointConfig(
            "${metricsUrl}v1/metrics",
            metricsHeaders + baseHeaders,
            expectedCompression,
            null
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
                null
            )
        config
            .logsEndpoint()
            .assertEndpointConfig(
                "${logUrl}v1/logs",
                logHeaders + baseHeaders,
                expectedCompression,
                null
            )
        config.metricsEndpoint().assertEndpointConfig(
            "${metricsUrl}v1/metrics",
            metricsHeaders + baseHeaders,
            expectedCompression,
            null
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

        config.spansEndpoint()
            .assertEndpointConfig(
                "${url}v1/traces",
                headers,
                Compression.GZIP,
                expectedSslContext
            )
        config.logsEndpoint()
            .assertEndpointConfig(
                "${url}v1/logs",
                headers,
                Compression.GZIP,
                expectedSslContext
            )
        config.metricsEndpoint()
            .assertEndpointConfig(
                "${url}v1/metrics",
                headers,
                Compression.GZIP,
                expectedSslContext
            )
    }

    @Test
    fun testClientTls() {
        val url = "http://localhost:4318/"
        val headers = mapOf("my-header" to "my-value")
        val expectedClientTls = getTestClientTls()
        val config =
            otelConfig.exportConfig.apply {
                baseUrl = url
                baseHeaders = headers
                clientTls = expectedClientTls
            }

        config.spansEndpoint()
            .assertEndpointConfig(
                "${url}v1/traces",
                headers,
                Compression.GZIP,
                null,
                expectedClientTls
            )
        config.logsEndpoint()
            .assertEndpointConfig(
                "${url}v1/logs",
                headers,
                Compression.GZIP,
                null,
                expectedClientTls
            )
        config.metricsEndpoint()
            .assertEndpointConfig(
                "${url}v1/metrics",
                headers,
                Compression.GZIP,
                null,
                expectedClientTls
            )
    }

    private fun HttpEndpointConnectivity.assertEndpointConfig(
        expectedUrl: String,
        expectedHeaders: Map<String, String>,
        expectedCompression: Compression,
        expectedSslContext: SSLContextConnectivity?,
        expectedClientTls: ClientTlsConnectivity? = null
    ) {
        assertEquals(expectedUrl, getUrl())
        assertEquals(expectedHeaders, getHeaders())
        assertEquals(expectedCompression, getCompression())
        assertEquals(expectedSslContext, getSslContext())
        assertEquals(expectedClientTls, getClientTls())
    }

    private fun getTestClientTls(): ClientTlsConnectivity {
        val privateKeyPem =
            """
            -----BEGIN PRIVATE KEY-----
            TEST_KEY
            -----END PRIVATE KEY-----
            """.trimIndent().toByteArray()

        val certificatePem =
            """
            -----BEGIN CERTIFICATE-----
            TEST_CERT
            -----END CERTIFICATE-----
            """.trimIndent().toByteArray()

        return ClientTlsConnectivity(privateKeyPem, certificatePem)
    }

  @Test
    fun testFullUrlOverrideForLogs() {
        val baseUrl = "http://localhost:4318/"
        val customLogsUrl = "http://localhost:4318/v2/logs"

        val config =
            otelConfig.exportConfig.apply {
                this.baseUrl = baseUrl
                logs {
                    fullUrl = customLogsUrl
                }
            }

        // logs should use the full custom URL without appending /v1/logs
        config.logsEndpoint().assertEndpointConfig(
            customLogsUrl,
            emptyMap(),
            Compression.GZIP,
            null
        )
        // spans and metrics should still use baseUrl + default path
        config.spansEndpoint().assertEndpointConfig(
            "${baseUrl}v1/traces",
            emptyMap(),
            Compression.GZIP,
            null
        )
        config.metricsEndpoint().assertEndpointConfig(
            "${baseUrl}v1/metrics",
            emptyMap(),
            Compression.GZIP,
            null
        )
    }

    @Test
    fun testFullUrlOverrideForAllSignals() {
        val customSpansUrl = "http://traces.example.com/v2/traces"
        val customLogsUrl = "http://logs.example.com/v2/logs"
        val customMetricsUrl = "http://metrics.example.com/v2/metrics"

        val config =
            otelConfig.exportConfig.apply {
                spans {
                    fullUrl = customSpansUrl
                }
                logs {
                    fullUrl = customLogsUrl
                }
                metrics {
                    fullUrl = customMetricsUrl
                }
            }

        config.spansEndpoint().assertEndpointConfig(
            customSpansUrl,
            emptyMap(),
            Compression.GZIP,
            null
        )
        config.logsEndpoint().assertEndpointConfig(
            customLogsUrl,
            emptyMap(),
            Compression.GZIP,
            null
        )
        config.metricsEndpoint().assertEndpointConfig(
            customMetricsUrl,
            emptyMap(),
            Compression.GZIP,
            null
        )
    }

    @Test
    fun testFullUrlTakesPrecedenceOverUrl() {
        val baseUrl = "http://localhost:4318/"
        val signalUrl = "http://localhost:4318/logs/"
        val customFullUrl = "http://localhost:4318/v2/logs"

        val config =
            otelConfig.exportConfig.apply {
                this.baseUrl = baseUrl
                logs {
                    url = signalUrl
                    fullUrl = customFullUrl
                }
            }

        // fullUrl should take precedence over url
        config.logsEndpoint().assertEndpointConfig(
            customFullUrl,
            emptyMap(),
            Compression.GZIP,
            null
        )
    }


}
