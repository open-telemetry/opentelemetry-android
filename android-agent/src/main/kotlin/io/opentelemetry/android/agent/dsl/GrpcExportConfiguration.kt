/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.GrpcEndpointConnectivity

@OpenTelemetryDslMarker
class GrpcExportConfiguration internal constructor() {
    var endpoint: String = ""

    var headers: Map<String, String> = emptyMap()

    var compression: Compression = Compression.GZIP

    private val spansConfig: EndpointConfiguration = EndpointConfiguration("")
    private val logsConfig: EndpointConfiguration = EndpointConfiguration("")
    private val metricsConfig: EndpointConfiguration = EndpointConfiguration("")

    internal fun spansEndpoint(): GrpcEndpointConnectivity =
        GrpcEndpointConnectivity.create(
            chooseEndpoint(spansConfig),
            spansConfig.headers + headers,
            chooseCompression(spansConfig.compression),
        )

    internal fun logsEndpoint(): GrpcEndpointConnectivity =
        GrpcEndpointConnectivity.create(
            chooseEndpoint(logsConfig),
            logsConfig.headers + headers,
            chooseCompression(logsConfig.compression),
        )

    internal fun metricsEndpoint(): GrpcEndpointConnectivity =
        GrpcEndpointConnectivity.create(
            chooseEndpoint(metricsConfig),
            metricsConfig.headers + headers,
            chooseCompression(metricsConfig.compression),
        )

    private fun chooseEndpoint(cfg: EndpointConfiguration): String = cfg.url.ifBlank { endpoint }

    private fun chooseCompression(signalConfigCompression: Compression?): Compression = signalConfigCompression ?: this.compression

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
