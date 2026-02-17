/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.GrpcEndpointConnectivity

/**
 * Configuration for exporting telemetry over gRPC.
 *
 * Use [endpoint], [headers], and [compression] for common settings, and
 * [spans], [logs], and [metrics] to override per-signal configuration as needed.
 */
@OpenTelemetryDslMarker
class GrpcExportConfiguration internal constructor() {
    /**
     * The base gRPC endpoint to which telemetry will be exported.
     *
     * This value is used for all signals (spans, logs, metrics) unless a
     * signal-specific endpoint is configured via [spans], [logs], or [metrics].
     */
    var endpoint: String = ""

    /**
     * Headers that will be sent with every gRPC export request.
     *
     * These headers are combined with any headers configured on the per-signal
     * [EndpointConfiguration] instances used by [spans], [logs], and [metrics].
     */
    var headers: Map<String, String> = emptyMap()

    /**
     * The compression algorithm to use for gRPC export requests.
     *
     * This acts as the default compression for all signals and may be overridden
     * in the per-signal [EndpointConfiguration].
     */
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
