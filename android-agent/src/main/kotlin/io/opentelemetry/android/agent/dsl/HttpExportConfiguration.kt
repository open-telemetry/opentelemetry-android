/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.connectivity.ClientTlsConnectivity
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity
import io.opentelemetry.android.agent.connectivity.SSLContextConnectivity

/**
 * Type-safe config DSL that controls how HTTP export of telemetry should behave.
 */
@OpenTelemetryDslMarker
class HttpExportConfiguration internal constructor() {
    /**
     * Global URL for HTTP export requests.
     */
    var baseUrl: String = ""

    /**
     * Global headers that should be attached to any HTTP export requests.
     */
    var baseHeaders: Map<String, String> = emptyMap()

    /**
     * Default compression algorithm for all export requests.
     */
    var compression: Compression = Compression.GZIP

    /**
     * Default SSL context for all export requests.
     */
    var sslContext: SSLContextConnectivity? = null

    /**
     * Sets ths client key and the certificate chain to use for verifying client
     * for all requests when TLS is enabled.
     */
    @Incubating
    var clientTls: ClientTlsConnectivity? = null

    private val spansConfig: EndpointConfiguration = EndpointConfiguration("")
    private val logsConfig: EndpointConfiguration = EndpointConfiguration("")
    private val metricsConfig: EndpointConfiguration = EndpointConfiguration("")

    internal fun spansEndpoint(): HttpEndpointConnectivity =
        HttpEndpointConnectivity.forTraces(
            chooseUrlSource(spansConfig),
            isFullUrl(spansConfig),
            spansConfig.headers + baseHeaders,
            chooseCompression(spansConfig.compression),
            sslContext,
            clientTls,
        )

    internal fun logsEndpoint(): HttpEndpointConnectivity =
        HttpEndpointConnectivity.forLogs(
            chooseUrlSource(logsConfig),
            isFullUrl(logsConfig),
            logsConfig.headers + baseHeaders,
            chooseCompression(logsConfig.compression),
            sslContext,
            clientTls,
        )

    internal fun metricsEndpoint(): HttpEndpointConnectivity =
        HttpEndpointConnectivity.forMetrics(
            chooseUrlSource(metricsConfig),
            isFullUrl(metricsConfig),
            metricsConfig.headers + baseHeaders,
            chooseCompression(metricsConfig.compression),
            sslContext,
            clientTls,
        )

    private fun chooseUrlSource(cfg: EndpointConfiguration): String =
        cfg.fullUrl ?: cfg.url.ifBlank { baseUrl }

    private fun isFullUrl(cfg: EndpointConfiguration): Boolean = cfg.fullUrl != null

    private fun chooseCompression(signalConfigCompression: Compression?): Compression =
        signalConfigCompression ?: this.compression

    /**
     * Override the default configuration for the v1/traces endpoint only.
     */
    fun spans(action: EndpointConfiguration.() -> Unit) {
        spansConfig.action()
    }

    /**
     * Override the default configuration for the v1/logs endpoint only.
     */
    fun logs(action: EndpointConfiguration.() -> Unit) {
        logsConfig.action()
    }

    /**
     * Override the default configuration for the v1/metrics endpoint only.
     */
    fun metrics(action: EndpointConfiguration.() -> Unit) {
        metricsConfig.action()
    }
}
