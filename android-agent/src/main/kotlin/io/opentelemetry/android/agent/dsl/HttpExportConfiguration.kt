/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity

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

    private val spansConfig: EndpointConfiguration = EndpointConfiguration("")
    private val logsConfig: EndpointConfiguration = EndpointConfiguration("")
    private val metricsConfig: EndpointConfiguration = EndpointConfiguration("")

    internal fun spansEndpoint(): HttpEndpointConnectivity =
        HttpEndpointConnectivity.forTraces(
            chooseUrlSource(spansConfig),
            spansConfig.headers + baseHeaders,
            chooseCompression(spansConfig.compression),
        )

    internal fun logsEndpoint(): HttpEndpointConnectivity =
        HttpEndpointConnectivity.forLogs(
            chooseUrlSource(logsConfig),
            logsConfig.headers + baseHeaders,
            chooseCompression(logsConfig.compression),
        )

    internal fun metricsEndpoint(): HttpEndpointConnectivity =
        HttpEndpointConnectivity.forMetrics(
            chooseUrlSource(metricsConfig),
            metricsConfig.headers + baseHeaders,
            chooseCompression(metricsConfig.compression),
        )

    private fun chooseUrlSource(cfg: EndpointConfiguration): String = cfg.url.ifBlank { baseUrl }

    private fun chooseCompression(signalConfigCompression: Compression?): Compression = signalConfigCompression ?: this.compression

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
