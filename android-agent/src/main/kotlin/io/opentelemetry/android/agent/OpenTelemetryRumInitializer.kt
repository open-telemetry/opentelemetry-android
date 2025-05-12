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
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.instrumentation.crash.CrashDetails
import io.opentelemetry.android.instrumentation.network.NetworkAttributesExtractor
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import java.time.Duration

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
        activityTracerCustomizer: ((Tracer) -> Tracer)? = null,
        activityNameExtractor: ScreenNameExtractor? = null,
        fragmentTracerCustomizer: ((Tracer) -> Tracer)? = null,
        fragmentNameExtractor: ScreenNameExtractor? = null,
        anrAttributesExtractors: List<AttributesExtractor<Array<StackTraceElement>, Void>> = emptyList(),
        crashAttributesExtractors: List<AttributesExtractor<CrashDetails, Void>> = emptyList(),
        networkChangeAttributesExtractors: List<NetworkAttributesExtractor> = emptyList(),
        slowRenderingDetectionPollInterval: Duration? = null,
    ): OpenTelemetryRum =
        OpenTelemetryRum
            .builder(application, rumConfig)
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
