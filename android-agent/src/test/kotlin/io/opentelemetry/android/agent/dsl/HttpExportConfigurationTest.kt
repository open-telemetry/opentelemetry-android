/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class HttpExportConfigurationTest {
    @Test
    fun testDefaults() {
        val config = HttpExportConfiguration()
        val expectedHeaders = emptyMap<String, String>()
        config.spansEndpoint().assertEndpointConfig("/v1/traces", expectedHeaders)
        config.logsEndpoint().assertEndpointConfig("/v1/logs", expectedHeaders)
        config.metricsEndpoint().assertEndpointConfig("/v1/metrics", expectedHeaders)
        assertEquals("", config.baseUrl)
        assertEquals(expectedHeaders, config.baseHeaders)
    }

    @Test
    fun testBaseValueOverride() {
        val url = "http://localhost:4318/"
        val headers = mapOf("my-header" to "my-value")
        val config =
            HttpExportConfiguration().apply {
                baseUrl = url
                baseHeaders = headers
            }

        config.spansEndpoint().assertEndpointConfig("${url}v1/traces", headers)
        config.logsEndpoint().assertEndpointConfig("${url}v1/logs", headers)
        config.metricsEndpoint().assertEndpointConfig("${url}v1/metrics", headers)
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

        val config =
            HttpExportConfiguration().apply {
                this.baseUrl = baseUrl
                this.baseHeaders = baseHeaders

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
            }

        config.spansEndpoint().assertEndpointConfig("${spanUrl}v1/traces", spanHeaders + baseHeaders)
        config.logsEndpoint().assertEndpointConfig("${logUrl}v1/logs", logHeaders + baseHeaders)
        config.metricsEndpoint().assertEndpointConfig(
            "${metricsUrl}v1/metrics",
            metricsHeaders + baseHeaders,
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

        val config =
            HttpExportConfiguration().apply {
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

        config.spansEndpoint().assertEndpointConfig("${spanUrl}v1/traces", spanHeaders + baseHeaders)
        config.logsEndpoint().assertEndpointConfig("${logUrl}v1/logs", logHeaders + baseHeaders)
        config.metricsEndpoint().assertEndpointConfig(
            "${metricsUrl}v1/metrics",
            metricsHeaders + baseHeaders,
        )
        assertEquals(baseUrl, config.baseUrl)
        assertEquals(baseHeaders, config.baseHeaders)
    }

    private fun HttpEndpointConnectivity.assertEndpointConfig(
        expectedUrl: String,
        expectedHeaders: Map<String, String>,
    ) {
        assertEquals(expectedUrl, getUrl())
        assertEquals(expectedHeaders, getHeaders())
    }
}
