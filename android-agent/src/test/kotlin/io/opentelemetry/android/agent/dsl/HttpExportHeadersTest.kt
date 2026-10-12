/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HttpExportHeadersTest {
    @Test
    fun `supplier is evaluated lazily for every signal and header lookup`() {
        var token = "initial"
        var calls = 0
        val config =
            HttpExportConfiguration().apply {
                headerSupplier {
                    calls++
                    mapOf("Authorization" to "Bearer $token")
                }
            }
        val endpoints = config.endpoints()
        assertThat(config.baseHeaders).isEmpty()
        assertThat(calls).isZero()

        endpoints.forEach {
            assertThat(it.getHeaders()).containsEntry("Authorization", "Bearer initial")
        }
        assertThat(calls).isEqualTo(3)

        token = "refreshed"
        endpoints.forEach {
            assertThat(it.getHeaders()).containsEntry("Authorization", "Bearer refreshed")
        }
        assertThat(calls).isEqualTo(6)
    }

    @Test
    fun `supplier merges with static and signal headers for every signal`() {
        val config =
            HttpExportConfiguration().apply {
                baseHeaders = mapOf("Authorization" to "base", "X-App" to "app", "X-Shared" to "base")
                spans { headers = mapOf("Authorization" to "span", "X-Signal" to "traces", "X-Shared" to "signal") }
                logs { headers = mapOf("Authorization" to "log", "X-Signal" to "logs", "X-Shared" to "signal") }
                metrics { headers = mapOf("Authorization" to "metric", "X-Signal" to "metrics", "X-Shared" to "signal") }
                headerSupplier { mapOf("Authorization" to "dynamic", "X-Dynamic" to "value") }
            }

        config.endpoints().zip(listOf("traces", "logs", "metrics")).forEach { (endpoint, signal) ->
            assertThat(endpoint.getHeaders()).isEqualTo(
                mapOf(
                    "Authorization" to "dynamic",
                    "X-App" to "app",
                    "X-Dynamic" to "value",
                    "X-Shared" to "base",
                    "X-Signal" to signal,
                ),
            )
        }
    }

    @Test
    fun `static header precedence is unchanged without a supplier`() {
        val config =
            HttpExportConfiguration().apply {
                baseHeaders = mapOf("Authorization" to "base")
                spans { headers = mapOf("Authorization" to "signal", "X-Signal" to "traces") }
                logs { headers = mapOf("Authorization" to "signal", "X-Signal" to "logs") }
                metrics { headers = mapOf("Authorization" to "signal", "X-Signal" to "metrics") }
            }

        assertThat(config.baseHeaders).containsEntry("Authorization", "base")
        config.endpoints().zip(listOf("traces", "logs", "metrics")).forEach { (endpoint, signal) ->
            assertThat(endpoint.getHeaders()).isEqualTo(mapOf("Authorization" to "base", "X-Signal" to signal))
        }
    }

    @Test
    fun `static headers merge with configured supplier`() {
        var calls = 0
        val config =
            HttpExportConfiguration().apply {
                headerSupplier {
                    calls++
                    mapOf("Authorization" to "dynamic")
                }
                baseHeaders = mapOf("Authorization" to "static")
            }

        config.endpoints().forEach {
            assertThat(it.getHeaders()).isEqualTo(mapOf("Authorization" to "dynamic"))
        }
        assertThat(calls).isEqualTo(3)
    }

    @Test
    fun `configuring supplier after static headers merges map`() {
        val config =
            HttpExportConfiguration().apply {
                baseHeaders = mapOf("Authorization" to "static")
                headerSupplier { emptyMap() }
            }

        assertThat(config.baseHeaders).containsEntry("Authorization", "static")
        config.endpoints().forEach {
            assertThat(it.getHeaders()).isEqualTo(config.baseHeaders)
        }
    }

    @Test
    fun `headers removed from supplier results do not persist`() {
        var dynamicHeaders = mapOf("Authorization" to "dynamic", "X-Temporary" to "value")
        val config =
            HttpExportConfiguration().apply {
                baseHeaders = mapOf("Authorization" to "static")
                headerSupplier { dynamicHeaders }
            }
        val endpoints = config.endpoints()
        endpoints.forEach {
            assertThat(it.getHeaders()).isEqualTo(dynamicHeaders)
        }

        dynamicHeaders = emptyMap()
        endpoints.forEach {
            assertThat(it.getHeaders()).isEqualTo(config.baseHeaders)
        }
    }

    @Test
    fun `configuring a second supplier replaces the first`() {
        var firstCalls = 0
        val config =
            HttpExportConfiguration().apply {
                headerSupplier {
                    firstCalls++
                    mapOf("Authorization" to "first")
                }
                headerSupplier { mapOf("Authorization" to "second") }
            }

        config.endpoints().forEach {
            assertThat(it.getHeaders()).isEqualTo(mapOf("Authorization" to "second"))
        }
        assertThat(firstCalls).isZero()
    }

    private fun HttpExportConfiguration.endpoints(): List<HttpEndpointConnectivity> =
        listOf(spansEndpoint(), logsEndpoint(), metricsEndpoint())
}
