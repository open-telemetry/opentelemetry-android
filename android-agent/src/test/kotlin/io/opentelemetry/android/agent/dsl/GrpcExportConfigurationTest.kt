/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.mockk
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.agent.OtlpTransport
import io.opentelemetry.android.agent.connectivity.ClientTlsConnectivity
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.SSLContextConnectivity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@OptIn(Incubating::class)
internal class GrpcExportConfigurationTest {
    private fun configuration(): OpenTelemetryConfiguration =
        OpenTelemetryConfiguration(clock = FakeClock(), instrumentationLoader = FakeInstrumentationLoader())

    @Test
    fun `HTTP remains the default and repeated gRPC blocks accumulate settings`() {
        val config = configuration()
        val http = config.resolveExportConfiguration()
        assertThat(http.transport).isEqualTo(OtlpTransport.HTTP)
        assertThat(http.spans.getUrl()).isEqualTo("/v1/traces")
        config.grpcExport {}
        val grpc = config.resolveExportConfiguration()
        assertThat(grpc.transport).isEqualTo(OtlpTransport.GRPC)
        listOf(grpc.spans, grpc.logs, grpc.metrics).forEach {
            assertThat(it.getUrl()).isEqualTo("http://localhost:4317")
            assertThat(it.getHeaders()).isEmpty()
            assertThat(it.getCompression()).isEqualTo(Compression.GZIP)
            assertThat(it.getSslContext()).isNull()
            assertThat(it.getClientTls()).isNull()
        }
        config.grpcExport { baseUrl = "https://collector.example.com:4317" }
        config.grpcExport { compression = Compression.NONE }
        val resolved = config.resolveExportConfiguration()
        assertThat(resolved.transport).isEqualTo(OtlpTransport.GRPC)
        assertThat(resolved.spans.getUrl()).isEqualTo("https://collector.example.com:4317")
        assertThat(resolved.spans.getCompression()).isEqualTo(Compression.NONE)
    }

    @Test
    fun `signal overrides preserve URLs and headers are refreshed with documented precedence`() {
        var token = "initial"
        val ssl = SSLContextConnectivity(mockk(), mockk())
        val config =
            GrpcExportConfiguration().apply {
                baseUrl = "https://collector.example.com:4317/"
                baseHeaders = mapOf("shared" to "global", "Authorization" to "static")
                sslContext = ssl
                headerSupplier { mapOf("Authorization" to token) }
                spans {
                    url = "https://traces.example.com:4317"
                    headers = mapOf("shared" to "signal", "signal" to "traces")
                    compression = Compression.NONE
                }
                logs { url = "https://logs.example.com:4317" }
                metrics { url = "https://metrics.example.com:4317" }
            }
        val spans = config.spansEndpoint()
        assertThat(spans.getUrl()).isEqualTo("https://traces.example.com:4317")
        assertThat(config.logsEndpoint().getUrl()).isEqualTo("https://logs.example.com:4317")
        assertThat(config.metricsEndpoint().getUrl()).isEqualTo("https://metrics.example.com:4317")
        assertThat(spans.getCompression()).isEqualTo(Compression.NONE)
        assertThat(spans.getHeaders()).containsAllEntriesOf(mapOf("shared" to "global", "signal" to "traces", "Authorization" to "initial"))
        token = "refreshed"
        listOf(spans, config.logsEndpoint(), config.metricsEndpoint()).forEach {
            assertThat(it.getHeaders()["Authorization"]).isEqualTo("refreshed")
            assertThat(it.getSslContext()).isSameAs(ssl)
            assertThat(it.getClientTls()).isNull()
        }
        config.logs { url = " " }
        assertThat(config.logsEndpoint().getUrl()).isEqualTo(config.baseUrl)
    }

    @Test
    fun `TLS configuration supports either SSL context or client credentials but not both`() {
        val sslContext = SSLContextConnectivity(mockk(), mockk())
        val sslConfiguration =
            configuration()
                .apply {
                    grpcExport { this.sslContext = sslContext }
                }.resolveExportConfiguration()
        assertThat(sslConfiguration.spans.getSslContext()).isSameAs(sslContext)
        assertThat(sslConfiguration.spans.getClientTls()).isNull()

        val clientTls = ClientTlsConnectivity(byteArrayOf(1), byteArrayOf(2))
        val clientTlsConfiguration =
            configuration()
                .apply {
                    grpcExport { this.clientTls = clientTls }
                }.resolveExportConfiguration()
        assertThat(clientTlsConfiguration.spans.getSslContext()).isNull()
        assertThat(clientTlsConfiguration.spans.getClientTls()).isSameAs(clientTls)

        val conflictingConfiguration =
            configuration().apply {
                grpcExport {
                    this.sslContext = sslContext
                    this.clientTls = clientTls
                }
            }
        assertThatThrownBy { conflictingConfiguration.resolveExportConfiguration() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("sslContext and clientTls cannot be combined")

        val conflictingHttpConfiguration =
            configuration().apply {
                httpExport {
                    this.sslContext = SSLContextConnectivity(mockk(), mockk())
                    this.clientTls = ClientTlsConnectivity(byteArrayOf(1), byteArrayOf(2))
                }
            }
        assertThatThrownBy { conflictingHttpConfiguration.resolveExportConfiguration() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("sslContext and clientTls cannot be combined")
    }

    @Test
    fun `configured endpoints are validated for enabled signals`() {
        val invalidHttp = configuration().apply { httpExport { baseUrl = "collector.example:4317" } }
        assertThatThrownBy { invalidHttp.resolveExportConfiguration() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageStartingWith("Invalid http endpoint")

        val invalidGrpc = configuration().apply { grpcExport { baseUrl = "collector.example:4317" } }
        assertThatThrownBy { invalidGrpc.resolveExportConfiguration() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageStartingWith("Invalid grpc endpoint")

        val emptyGrpc = configuration().apply { grpcExport { baseUrl = "" } }
        assertThatThrownBy { emptyGrpc.resolveExportConfiguration() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageStartingWith("Invalid grpc endpoint")

        val disabledHttpSignal =
            configuration().apply {
                disableTracing()
                httpExport { spans { fullUrl = "collector.example:4317" } }
            }
        disabledHttpSignal.resolveExportConfiguration()

        val disabledGrpcSignal =
            configuration().apply {
                disableTracing()
                grpcExport { spans { url = "collector.example:4317" } }
            }
        disabledGrpcSignal.resolveExportConfiguration()
    }

    @Test
    fun `repeated HTTP blocks retain settings without exposing transport state`() {
        val config = configuration()
        config.httpExport { baseUrl = "https://collector.example.com" }
        config.httpExport { baseHeaders = mapOf("X-App" to "test") }
        val resolved = config.resolveExportConfiguration()
        assertThat(resolved.transport).isEqualTo(OtlpTransport.HTTP)
        assertThat(resolved.spans.getUrl()).isEqualTo("https://collector.example.com/v1/traces")
        assertThat(resolved.logs.getHeaders()).containsEntry("X-App", "test")
    }

    @Test
    fun `mixed protocols are rejected in either order`() {
        var conflictingActions = 0
        val httpFirst = configuration().apply { httpExport {} }
        assertThatThrownBy { httpFirst.grpcExport { conflictingActions++ } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("httpExport and grpcExport cannot be combined")
        val grpcFirst = configuration().apply { grpcExport {} }
        assertThatThrownBy { grpcFirst.httpExport { conflictingActions++ } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("httpExport and grpcExport cannot be combined")
        assertThat(conflictingActions).isZero()
        assertThat(httpFirst.resolveExportConfiguration().transport).isEqualTo(OtlpTransport.HTTP)
        assertThat(grpcFirst.resolveExportConfiguration().transport).isEqualTo(OtlpTransport.GRPC)
    }
}
