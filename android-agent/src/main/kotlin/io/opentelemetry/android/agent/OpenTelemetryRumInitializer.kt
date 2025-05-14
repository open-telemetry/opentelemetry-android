/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.OpenTelemetryRumBuilder
import io.opentelemetry.android.agent.connectivity.EndpointConnectivity
import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.agent.session.SessionManager
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter

object OpenTelemetryRumInitializer {
    /**
     * Opinionated [OpenTelemetryRum] initialization.
     *
     * @param application Your android app's application object.
     * @param endpointBaseUrl The base endpoint for exporting all your signals.
     * @param endpointHeaders These will be added to each signal export request.
     * @param spanEndpointConnectivity Span-specific endpoint configuration.
     * @param logEndpointConnectivity Log-specific endpoint configuration.
     * @param metricEndpointConnectivity Metric-specific endpoint configuration.
     * @param rumConfig Configuration used by [OpenTelemetryRumBuilder].
     */
    @JvmStatic
    fun initialize(
        application: Application,
        endpointBaseUrl: String,
        endpointHeaders: Map<String, String> = emptyMap(),
        spanEndpointConnectivity: EndpointConnectivity =
            HttpEndpointConnectivity.forTraces(
                endpointBaseUrl,
                endpointHeaders,
            ),
        logEndpointConnectivity: EndpointConnectivity =
            HttpEndpointConnectivity.forLogs(
                endpointBaseUrl,
                endpointHeaders,
            ),
        metricEndpointConnectivity: EndpointConnectivity =
            HttpEndpointConnectivity.forMetrics(
                endpointBaseUrl,
                endpointHeaders,
            ),
        rumConfig: OtelRumConfig = OtelRumConfig(),
        sessionConfig: SessionConfig = SessionConfig(),
    ): OpenTelemetryRum {
        // The session manager will be a SessionProvider implementation living in the agent where the agent will
        // have full control of what a session is and will also make it observable and configurable.
        val sessionManager =
            SessionManager.create(SessionIdTimeoutHandler(sessionConfig), sessionConfig)

        // We would have control to initialize the agent's session whenever we see fit, such as
        // during initialization, e.g:
        //
        // sessionManager.startSession()

        return OpenTelemetryRum
            .builder(application, rumConfig)
            .setSessionProvider(sessionManager)
            .addSpanExporterCustomizer {
                OtlpHttpSpanExporter
                    .builder()
                    .setEndpoint(spanEndpointConnectivity.getUrl())
                    .setHeaders(spanEndpointConnectivity::getHeaders)
                    .build()
            }.addLogRecordExporterCustomizer {
                OtlpHttpLogRecordExporter
                    .builder()
                    .setEndpoint(logEndpointConnectivity.getUrl())
                    .setHeaders(logEndpointConnectivity::getHeaders)
                    .build()
            }.addMetricExporterCustomizer {
                OtlpHttpMetricExporter
                    .builder()
                    .setEndpoint(metricEndpointConnectivity.getUrl())
                    .setHeaders(metricEndpointConnectivity::getHeaders)
                    .build()
            }.build()
    }
}
