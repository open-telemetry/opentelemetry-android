/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference

internal class DynamicHeadersExportTest {
    @Test
    fun `refreshed tokens are sent by all exporters without reinitializing`() {
        MockWebServer().use { server ->
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse = MockResponse(200)
                }
            server.start()
            val token = AtomicReference("initial")
            val config =
                HttpExportConfiguration().apply {
                    baseUrl = server.url("/").toString()
                    baseHeaders = mapOf("X-App" to "test")
                    headerSupplier {
                        mapOf("Authorization" to "Bearer ${token.get()}")
                    }
                }
            config.createSdk().use { sdk ->
                val tracer = sdk.getTracer("dynamic-headers-test")
                val logger = sdk.logsBridge.get("dynamic-headers-test")
                val counter = sdk.getMeter("dynamic-headers-test").counterBuilder("test.counter").build()

                listOf("initial", "refreshed").forEach { currentToken ->
                    token.set(currentToken)
                    tracer.spanBuilder("test").startSpan().end()
                    logger.logRecordBuilder().setBody("test").emit()
                    counter.add(1)
                    assertThat(
                        sdk.sdkTracerProvider
                            .forceFlush()
                            .join(10, SECONDS)
                            .isSuccess,
                    ).isTrue()
                    assertThat(
                        sdk.sdkLoggerProvider
                            .forceFlush()
                            .join(10, SECONDS)
                            .isSuccess,
                    ).isTrue()
                    assertThat(
                        sdk.sdkMeterProvider
                            .forceFlush()
                            .join(10, SECONDS)
                            .isSuccess,
                    ).isTrue()

                    val paths = mutableListOf<String>()
                    repeat(3) {
                        val request = checkNotNull(server.takeRequest(10, SECONDS))
                        paths.add(request.target)
                        assertThat(request.headers["Authorization"]).isEqualTo("Bearer $currentToken")
                        assertThat(request.headers["X-App"]).isEqualTo("test")
                    }
                    assertThat(paths).containsExactlyInAnyOrder("/v1/traces", "/v1/logs", "/v1/metrics")
                }
            }
        }
    }

    private fun HttpExportConfiguration.createSdk(): OpenTelemetrySdk {
        val spans = spansEndpoint()
        val logs = logsEndpoint()
        val metrics = metricsEndpoint()
        val spanExporter =
            OtlpHttpSpanExporter
                .builder()
                .setEndpoint(spans.getUrl())
                .setHeaders(spans::getHeaders)
                .build()
        val logExporter =
            OtlpHttpLogRecordExporter
                .builder()
                .setEndpoint(logs.getUrl())
                .setHeaders(logs::getHeaders)
                .build()
        val metricExporter =
            OtlpHttpMetricExporter
                .builder()
                .setEndpoint(metrics.getUrl())
                .setHeaders(metrics::getHeaders)
                .build()
        return OpenTelemetrySdk
            .builder()
            .setTracerProvider(
                SdkTracerProvider
                    .builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                    .build(),
            ).setLoggerProvider(
                SdkLoggerProvider
                    .builder()
                    .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
                    .build(),
            ).setMeterProvider(
                SdkMeterProvider
                    .builder()
                    .registerMetricReader(
                        PeriodicMetricReader.builder(metricExporter).setInterval(1, DAYS).build(),
                    ).build(),
            ).build()
    }
}
