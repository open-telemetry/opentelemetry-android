/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.connectivity.ClientTlsConnectivity
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.EndpointConnectivity
import io.opentelemetry.android.agent.connectivity.SSLContextConnectivity

/** Configures OTLP export over gRPC for traces, logs, and metrics. */
@OpenTelemetryDslMarker
class GrpcExportConfiguration internal constructor() {
    /** Collector URL shared by all signals. HTTP signal paths such as /v1/traces are not appended. */
    var baseUrl: String = "http://localhost:4317"

    /** Static global headers, which override signal-specific headers with the same key. */
    var baseHeaders: Map<String, String> = emptyMap()

    /** Default compression for all signals. */
    var compression: Compression = Compression.GZIP

    /** Optional SSL context and trust manager for all signals. Cannot be combined with [clientTls]. */
    var sslContext: SSLContextConnectivity? = null

    /** Client private key and certificate chain in PEM format for mutual TLS. Cannot be combined with [sslContext]. */
    @Incubating
    var clientTls: ClientTlsConnectivity? = null

    private var headersSupplier: () -> Map<String, String> = { emptyMap() }
    private val spansConfig = GrpcEndpointConfiguration()
    private val logsConfig = GrpcEndpointConfiguration()
    private val metricsConfig = GrpcEndpointConfiguration()

    /**
     * Supplies headers for each export request. Supplied values override static headers.
     * Must be thread-safe and non-blocking; read a cached token rather than refreshing it here.
     */
    fun headerSupplier(supplier: () -> Map<String, String>) {
        headersSupplier = supplier
    }

    /** Overrides the global settings for traces. */
    fun spans(action: GrpcEndpointConfiguration.() -> Unit) {
        spansConfig.action()
    }

    /** Overrides the global settings for logs. */
    fun logs(action: GrpcEndpointConfiguration.() -> Unit) {
        logsConfig.action()
    }

    /** Overrides the global settings for metrics. */
    fun metrics(action: GrpcEndpointConfiguration.() -> Unit) {
        metricsConfig.action()
    }

    @OptIn(Incubating::class)
    internal fun validate() {
        require(sslContext == null || clientTls == null) {
            "sslContext and clientTls cannot be combined"
        }
    }

    internal fun spansEndpointUrl(): String = spansConfig.url.ifBlank { baseUrl }

    internal fun logsEndpointUrl(): String = logsConfig.url.ifBlank { baseUrl }

    internal fun metricsEndpointUrl(): String = metricsConfig.url.ifBlank { baseUrl }

    internal fun spansEndpoint(): EndpointConnectivity = endpoint(spansConfig)

    internal fun logsEndpoint(): EndpointConnectivity = endpoint(logsConfig)

    internal fun metricsEndpoint(): EndpointConnectivity = endpoint(metricsConfig)

    @OptIn(Incubating::class)
    private fun endpoint(config: GrpcEndpointConfiguration): EndpointConnectivity {
        val url = config.url.ifBlank { baseUrl }
        val compression = config.compression ?: this.compression
        val sslContext = this.sslContext
        val clientTls = this.clientTls
        return object : EndpointConnectivity {
            override fun getUrl(): String = url

            override fun getHeaders(): Map<String, String> = config.headers + baseHeaders + headersSupplier()

            override fun getCompression(): Compression = compression

            override fun getSslContext(): SSLContextConnectivity? = sslContext

            override fun getClientTls(): ClientTlsConnectivity? = clientTls
        }
    }
}
