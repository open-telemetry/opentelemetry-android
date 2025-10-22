/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity

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

    private val spansConfig: EndpointConfiguration = EndpointConfiguration("")
    private val logsConfig: EndpointConfiguration = EndpointConfiguration("")
    private val metricsConfig: EndpointConfiguration = EndpointConfiguration("")

    internal fun spansEndpoint(): HttpEndpointConnectivity =
        HttpEndpointConnectivity.forTraces(
            chooseUrlSource(spansConfig),
            spansConfig.headers + baseHeaders,
        )

    internal fun logsEndpoint(): HttpEndpointConnectivity =
        HttpEndpointConnectivity.forLogs(
            chooseUrlSource(logsConfig),
            logsConfig.headers + baseHeaders,
        )

    internal fun metricsEndpoint(): HttpEndpointConnectivity =
        HttpEndpointConnectivity.forMetrics(
            chooseUrlSource(metricsConfig),
            metricsConfig.headers + baseHeaders,
        )

    private fun chooseUrlSource(cfg: EndpointConfiguration): String = cfg.url.ifBlank { baseUrl }

    fun spans(action: EndpointConfiguration.() -> Unit) {
        spansConfig.action()
    }

    fun logs(action: EndpointConfiguration.() -> Unit) {
        logsConfig.action()
    }

    fun metrics(action: EndpointConfiguration.() -> Unit) {
        metricsConfig.action()
    }
}
