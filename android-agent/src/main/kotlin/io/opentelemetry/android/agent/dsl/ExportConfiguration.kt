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

/**
 * Unified configuration for exporting telemetry that supports both HTTP and gRPC protocols.
 *
 * This is the preferred way to configure telemetry export as it provides a single entry point
 * for both transport protocols. Use [protocol] to select the transport, and [endpoint], [headers],
 * and [compression] for common settings. Use [spans], [logs], and [metrics] to override
 * per-signal configuration as needed.
 */
@OpenTelemetryDslMarker
class ExportConfiguration internal constructor() {
    /**
     * Export protocol to use for all telemetry signals.
     *
     * This value controls whether HTTP or gRPC exporters are created. Defaults to [ExportProtocol.HTTP].
     */
    var protocol: ExportProtocol = ExportProtocol.HTTP

    /**
     * Default endpoint URL used for all telemetry signals.
     *
     * Each signal ([spans], [logs], [metrics]) can define its own endpoint.
     * If a signal-specific endpoint is blank, this global endpoint is used instead.
     */
    var endpoint: String = ""

    /**
     * Global headers applied to all telemetry exports.
     *
     * These headers are merged with signal-specific headers configured via
     * [EndpointConfiguration.headers].
     */
    var headers: Map<String, String> = emptyMap()

    /**
     * Default compression algorithm for all telemetry signals.
     *
     * A signal can override this by setting a non-null compression in its
     * [EndpointConfiguration].
     */
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

    /**
     * Configures export options specific to span data.
     *
     * Values set in this configuration override the top-level [endpoint],
     * [headers], and [compression] for span exports.
     */
    fun spans(action: EndpointConfiguration.() -> Unit) {
        spansConfig.action()
    }

    /**
     * Configures export options specific to log data.
     *
     * Values set in this configuration override the top-level [endpoint],
     * [headers], and [compression] for log exports.
     */
    fun logs(action: EndpointConfiguration.() -> Unit) {
        logsConfig.action()
    }

    /**
     * Configures export options specific to metric data.
     *
     * Values set in this configuration override the top-level [endpoint],
     * [headers], and [compression] for metric exports.
     */
    fun metrics(action: EndpointConfiguration.() -> Unit) {
        metricsConfig.action()
    }
}
