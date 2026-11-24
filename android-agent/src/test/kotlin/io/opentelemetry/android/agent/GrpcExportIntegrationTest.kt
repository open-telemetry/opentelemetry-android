/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import okhttp3.Protocol
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.TimeUnit.SECONDS

@OptIn(Incubating::class)
@RunWith(RobolectricTestRunner::class)
class GrpcExportIntegrationTest {
    @Test
    fun `DSL routes all signals to their gRPC endpoints and preserves host filters`() {
        server().use { traces ->
            server().use { logs ->
                server().use { metrics ->
                    val rum = createRum(traces, logs, metrics)
                    try {
                        recordTelemetry(rum)
                        rum.shutdown()
                        assertExported(traces, logs, metrics)
                    } finally {
                        rum.shutdown()
                    }
                }
            }
        }
    }

    private fun createRum(
        traces: MockWebServer,
        logs: MockWebServer,
        metrics: MockWebServer,
    ) = OpenTelemetryRumInitializer.initialize(RuntimeEnvironment.getApplication()) {
        rumConfig.disableInstrumentationDiscovery().disableNetworkAttributes().disableScreenAttributes()
        diskBuffering { enabled(false) }
        grpcExport {
            baseUrl = "http://unused.invalid:4317"
            compression = Compression.NONE
            headerSupplier { mapOf("Authorization" to "Bearer test") }
            spans { url = traces.url("/").toString() }
            logs { url = logs.url("/").toString() }
            metrics { url = metrics.url("/").toString() }
        }
        instrumentations {
            httpTelemetry {
                shouldRecordSpanForHost { it == "keep.example" }
                shouldRecordWebsocketEventForHost { it == "keep.example" }
            }
        }
    }

    private fun recordTelemetry(rum: io.opentelemetry.android.OpenTelemetryRum) {
        val openTelemetry = rum.openTelemetry
        listOf("keep.example", "drop.example").forEach { host ->
            openTelemetry.tracerProvider
                .get("test")
                .spanBuilder(host)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("http.request.method", "GET")
                .setAttribute("server.address", host)
                .startSpan()
                .end()
            openTelemetry.logsBridge
                .get("io.opentelemetry.websocket.events")
                .logRecordBuilder()
                .setEventName("websocket.open")
                .setAttribute(stringKey("server.address"), host)
                .setBody(host)
                .emit()
        }
        openTelemetry.meterProvider
            .get("test")
            .counterBuilder("test.counter")
            .build()
            .add(1)
    }

    private fun assertExported(
        traces: MockWebServer,
        logs: MockWebServer,
        metrics: MockWebServer,
    ) {
        val spans =
            ExportTraceServiceRequest
                .parseFrom(payload(traces, "trace", "Trace"))
                .resourceSpansList
                .flatMap { it.scopeSpansList }
                .flatMap { it.spansList }
        assertThat(spans.map { it.name }).containsExactly("keep.example")
        val records =
            ExportLogsServiceRequest
                .parseFrom(payload(logs, "logs", "Logs"))
                .resourceLogsList
                .flatMap { it.scopeLogsList }
                .flatMap { it.logRecordsList }
        assertThat(records.map { it.body.stringValue }).containsExactly("keep.example")
        val measurements =
            ExportMetricsServiceRequest
                .parseFrom(payload(metrics, "metrics", "Metrics"))
                .resourceMetricsList
                .flatMap { it.scopeMetricsList }
                .flatMap { it.metricsList }
        assertThat(measurements.map { it.name }).contains("test.counter")
    }

    @Test
    fun `disabled signals do not construct gRPC exporters`() {
        val rum =
            OpenTelemetryRumInitializer.initialize(RuntimeEnvironment.getApplication()) {
                rumConfig.disableInstrumentationDiscovery()
                diskBuffering { enabled(false) }
                disableTracing()
                disableLogging()
                disableMetrics()
                grpcExport { baseUrl = "not-a-url" }
            }
        rum.shutdown()
    }

    private fun server(): MockWebServer =
        MockWebServer().apply {
            protocols = listOf(Protocol.H2_PRIOR_KNOWLEDGE)
            repeat(4) {
                enqueue(
                    MockResponse
                        .Builder()
                        .setHeader("Content-Type", "application/grpc")
                        .body(Buffer().write(byteArrayOf(0, 0, 0, 0, 0)))
                        .trailers(Headers.headersOf("grpc-status", "0"))
                        .build(),
                )
            }
            start()
        }

    private fun payload(
        server: MockWebServer,
        signal: String,
        service: String,
    ): ByteArray {
        val request = checkNotNull(server.takeRequest(10, SECONDS))
        assertThat(request.target).isEqualTo("/opentelemetry.proto.collector.$signal.v1.${service}Service/Export")
        assertThat(request.version).isEqualTo("HTTP/2")
        assertThat(request.headers["Authorization"]).isEqualTo("Bearer test")
        val body = Buffer().write(checkNotNull(request.body))
        assertThat(body.readByte().toInt()).isZero()
        assertThat(body.readInt().toLong()).isEqualTo(body.size)
        return body.readByteArray()
    }
}
