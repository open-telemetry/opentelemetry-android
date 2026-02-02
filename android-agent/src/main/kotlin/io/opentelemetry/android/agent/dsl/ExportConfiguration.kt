/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.EndpointConnectivity
import io.opentelemetry.android.agent.connectivity.ExportProtocol
import io.opentelemetry.android.agent.connectivity.GrpcEndpointConnectivity
import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity

@OpenTelemetryDslMarker
class ExportConfiguration internal constructor() {
    var protocol: ExportProtocol = ExportProtocol.HTTP

    var endpoint: String = ""

    var headers: Map<String, String> = emptyMap()

    var compression: Compression = Compression.GZIP

    private val spansConfig: EndpointConfiguration = EndpointConfiguration("")
    private val logsConfig: EndpointConfiguration = EndpointConfiguration("")
    private val metricsConfig: EndpointConfiguration = EndpointConfiguration("")

    internal fun spansEndpoint(): EndpointConnectivity =
        when (protocol) {
            ExportProtocol.HTTP -> {
                HttpEndpointConnectivity.forTraces(
                    chooseEndpoint(spansConfig),
                    spansConfig.headers + headers,
                    chooseCompression(spansConfig.compression),
                )
            }

            ExportProtocol.GRPC -> {
                GrpcEndpointConnectivity.create(
                    chooseEndpoint(spansConfig),
                    spansConfig.headers + headers,
                    chooseCompression(spansConfig.compression),
                )
            }
        }

    internal fun logsEndpoint(): EndpointConnectivity =
        when (protocol) {
            ExportProtocol.HTTP -> {
                HttpEndpointConnectivity.forLogs(
                    chooseEndpoint(logsConfig),
                    logsConfig.headers + headers,
                    chooseCompression(logsConfig.compression),
                )
            }

            ExportProtocol.GRPC -> {
                GrpcEndpointConnectivity.create(
                    chooseEndpoint(logsConfig),
                    logsConfig.headers + headers,
                    chooseCompression(logsConfig.compression),
                )
            }
        }

    internal fun metricsEndpoint(): EndpointConnectivity =
        when (protocol) {
            ExportProtocol.HTTP -> {
                HttpEndpointConnectivity.forMetrics(
                    chooseEndpoint(metricsConfig),
                    metricsConfig.headers + headers,
                    chooseCompression(metricsConfig.compression),
                )
            }

            ExportProtocol.GRPC -> {
                GrpcEndpointConnectivity.create(
                    chooseEndpoint(metricsConfig),
                    metricsConfig.headers + headers,
                    chooseCompression(metricsConfig.compression),
                )
            }
        }

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
