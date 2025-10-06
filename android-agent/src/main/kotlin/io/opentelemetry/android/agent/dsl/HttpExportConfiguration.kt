/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity

@OpenTelemetryDslMarker
class HttpExportConfiguration {
    /**
     * Global URL for HTTP export requests.
     */
    var baseUrl: String = ""

    /**
     * Global headers that should be attached to any HTTP export requests.
     */
    var baseHeaders: Map<String, String> = emptyMap()

    internal val spansConfig: EndpointConfiguration =
        HttpEndpointConnectivity
            .forTraces(
                baseUrl,
                baseHeaders,
            ).let(::toEndpointConfiguration)

    internal val logsConfig: EndpointConfiguration =
        HttpEndpointConnectivity
            .forLogs(
                baseUrl,
                baseHeaders,
            ).let(::toEndpointConfiguration)

    internal val metricsConfig: EndpointConfiguration =
        HttpEndpointConnectivity
            .forMetrics(
                baseUrl,
                baseHeaders,
            ).let(::toEndpointConfiguration)

    fun spans(action: EndpointConfiguration.() -> Unit) {
        spansConfig.action()
    }

    fun logs(action: EndpointConfiguration.() -> Unit) {
        logsConfig.action()
    }

    fun metrics(action: EndpointConfiguration.() -> Unit) {
        metricsConfig.action()
    }

    private fun toEndpointConfiguration(connectivity: HttpEndpointConnectivity): EndpointConfiguration =
        EndpointConfiguration(connectivity.getUrl(), connectivity.getHeaders())
}
