/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.connectivity.ClientTlsConnectivity
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.EndpointConnectivity
import io.opentelemetry.android.agent.connectivity.SSLContextConnectivity
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Headers
import okhttp3.Protocol
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPInputStream

@OptIn(Incubating::class)
internal class OtlpExporterFactoryTest {
    @TestFactory
    fun `all signals export with refreshed headers over HTTP and gRPC`(): List<DynamicTest> =
        listOf(false, true).flatMap { grpc ->
            Compression.entries.map { compression ->
                DynamicTest.dynamicTest("grpc=$grpc compression=$compression") {
                    verifyExport(grpc, compression)
                }
            }
        }

    private fun verifyExport(
        grpc: Boolean,
        compression: Compression,
    ) {
        MockWebServer().use { server ->
            if (grpc) {
                server.protocols = listOf(Protocol.H2_PRIOR_KNOWLEDGE)
            }
            repeat(6) {
                server.enqueue(
                    if (grpc) {
                        MockResponse
                            .Builder()
                            .setHeader("Content-Type", "application/grpc")
                            .body(Buffer().write(byteArrayOf(0, 0, 0, 0, 0)))
                            .trailers(Headers.headersOf("grpc-status", "0"))
                            .build()
                    } else {
                        MockResponse(200)
                    },
                )
            }
            server.start()
            val token = AtomicReference("initial")
            val endpoint =
                object : EndpointConnectivity {
                    override fun getUrl(): String = server.url("/").toString()

                    override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer ${token.get()}", "X-App" to "test")

                    override fun getCompression(): Compression = compression

                    override fun getSslContext(): SSLContextConnectivity? = null

                    override fun getClientTls(): ClientTlsConnectivity? = null
                }
            createSdk(endpoint, grpc).use { sdk ->
                val tracer = sdk.getTracer("export-test")
                val logger = sdk.logsBridge.get("export-test")
                val counter = sdk.getMeter("export-test").counterBuilder("test.counter").build()
                listOf("initial", "refreshed").forEach { currentToken ->
                    token.set(currentToken)
                    tracer.spanBuilder("test-span").startSpan().end()
                    logger.logRecordBuilder().setBody("test-log").emit()
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

                    val requests = List(3) { checkNotNull(server.takeRequest(10, SECONDS)) }
                    assertRequests(requests, grpc, compression, currentToken)
                }
            }
        }
    }

    private fun assertRequests(
        requests: List<RecordedRequest>,
        grpc: Boolean,
        compression: Compression,
        token: String,
    ) {
        val paths =
            if (grpc) {
                listOf("trace", "logs", "metrics").map { signal ->
                    val service = signal.replaceFirstChar { it.uppercase() }
                    "/opentelemetry.proto.collector.$signal.v1.${service}Service/Export"
                }
            } else {
                listOf("/v1/traces", "/v1/logs", "/v1/metrics")
            }
        assertThat(requests.map { it.target }).containsExactlyInAnyOrderElementsOf(paths)
        requests.forEach { request ->
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.headers["Authorization"]).isEqualTo("Bearer $token")
            assertThat(request.headers["X-App"]).isEqualTo("test")
            val encodingHeader = if (grpc) "grpc-encoding" else "Content-Encoding"
            if (compression == Compression.GZIP) {
                assertThat(request.headers[encodingHeader]).isEqualTo("gzip")
            } else {
                assertThat(request.headers[encodingHeader]).isIn(null, "identity")
            }
            assertPayload(request, grpc, compression)
        }
    }

    private fun assertPayload(
        request: RecordedRequest,
        grpc: Boolean,
        compression: Compression,
    ) {
        val body = Buffer().write(checkNotNull(request.body))
        if (grpc) {
            assertThat(request.version).isEqualTo("HTTP/2")
            assertThat(request.headers["Content-Type"]).startsWith("application/grpc")
            assertThat(body.readByte().toInt()).isEqualTo(if (compression == Compression.GZIP) 1 else 0)
            assertThat(body.readInt().toLong()).isEqualTo(body.size)
        }
        val payload =
            if (compression == Compression.GZIP) {
                GZIPInputStream(body.inputStream()).use { it.readBytes() }
            } else {
                body.readByteArray()
            }
        when {
            request.target.contains("TraceService") || request.target == "/v1/traces" -> {
                val spans =
                    ExportTraceServiceRequest
                        .parseFrom(payload)
                        .resourceSpansList
                        .flatMap { it.scopeSpansList }
                        .flatMap { it.spansList }
                assertThat(spans.map { it.name }).containsExactly("test-span")
            }

            request.target.contains("LogsService") || request.target == "/v1/logs" -> {
                val logs =
                    ExportLogsServiceRequest
                        .parseFrom(payload)
                        .resourceLogsList
                        .flatMap { it.scopeLogsList }
                        .flatMap { it.logRecordsList }
                assertThat(logs.map { it.body.stringValue }).containsExactly("test-log")
            }

            else -> {
                val metrics =
                    ExportMetricsServiceRequest
                        .parseFrom(payload)
                        .resourceMetricsList
                        .flatMap { it.scopeMetricsList }
                        .flatMap { it.metricsList }
                assertThat(metrics.map { it.name }).contains("test.counter")
            }
        }
    }

    private fun createSdk(
        endpoint: EndpointConnectivity,
        grpc: Boolean,
    ): OpenTelemetrySdk {
        val transport = if (grpc) OtlpTransport.GRPC else OtlpTransport.HTTP
        val spanEndpoint = if (grpc) endpoint else httpEndpoint(endpoint, "traces")
        val logsEndpoint = if (grpc) endpoint else httpEndpoint(endpoint, "logs")
        val metricsEndpoint = if (grpc) endpoint else httpEndpoint(endpoint, "metrics")
        val spanExporter = OtlpExporterFactory.createSpanExporter(transport, spanEndpoint)
        val logExporter = OtlpExporterFactory.createLogExporter(transport, logsEndpoint)
        val metricExporter = OtlpExporterFactory.createMetricExporter(transport, metricsEndpoint)
        return OpenTelemetrySdk
            .builder()
            .setTracerProvider(
                SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spanExporter)).build(),
            ).setLoggerProvider(
                SdkLoggerProvider.builder().addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter)).build(),
            ).setMeterProvider(
                SdkMeterProvider
                    .builder()
                    .registerMetricReader(
                        PeriodicMetricReader.builder(metricExporter).setInterval(1, DAYS).build(),
                    ).build(),
            ).build()
    }

    private fun httpEndpoint(
        endpoint: EndpointConnectivity,
        signal: String,
    ): EndpointConnectivity =
        object : EndpointConnectivity by endpoint {
            override fun getUrl(): String = endpoint.getUrl() + "v1/$signal"
        }

    @Test
    fun `gRPC exporters reject invalid endpoints`() {
        val endpoint =
            object : EndpointConnectivity {
                override fun getUrl(): String = "not-a-url"

                override fun getHeaders(): Map<String, String> = emptyMap()

                override fun getCompression(): Compression = Compression.GZIP

                override fun getSslContext(): SSLContextConnectivity? = null

                override fun getClientTls(): ClientTlsConnectivity? = null
            }
        assertThatThrownBy { OtlpExporterFactory.createGrpcSpanExporter(endpoint) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { OtlpExporterFactory.createGrpcLogExporter(endpoint) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { OtlpExporterFactory.createGrpcMetricExporter(endpoint) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
