/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Application
import android.content.Context
import io.opentelemetry.android.AndroidResource
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.RumBuilder
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.EndpointConnectivity
import io.opentelemetry.android.agent.connectivity.ExportProtocol
import io.opentelemetry.android.agent.dsl.OpenTelemetryConfiguration
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.agent.session.SessionManager
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.trace.export.SpanExporter

@OptIn(Incubating::class)
object OpenTelemetryRumInitializer {
    @JvmStatic
    fun initialize(
        context: Context,
        configuration: (OpenTelemetryConfiguration.() -> Unit) = {},
    ): OpenTelemetryRum {
        val cfg = OpenTelemetryConfiguration()
        configuration(cfg)

        val ctx =
            when (context) {
                is Application -> context
                else -> context.applicationContext ?: context
            }

        val exportEndpoints = resolveExportEndpoints(cfg)

        val resourceBuilder = AndroidResource.createDefault(ctx).toBuilder()
        cfg.resourceAction(resourceBuilder)
        val resource = resourceBuilder.build()

        return RumBuilder
            .builder(ctx, cfg.rumConfig)
            .setSessionProvider(createSessionProvider(ctx, cfg))
            .setResource(resource)
            .setClock(cfg.clock)
            .addSpanExporterCustomizer {
                createSpanExporter(exportEndpoints.spans, exportEndpoints.protocol)
            }.addLogRecordExporterCustomizer {
                createLogRecordExporter(exportEndpoints.logs, exportEndpoints.protocol)
            }.addMetricExporterCustomizer {
                createMetricExporter(exportEndpoints.metrics, exportEndpoints.protocol)
            }.build()
    }

    private data class ExportEndpoints(
        val spans: EndpointConnectivity,
        val logs: EndpointConnectivity,
        val metrics: EndpointConnectivity,
        val protocol: ExportProtocol,
    )

    private fun resolveExportEndpoints(cfg: OpenTelemetryConfiguration): ExportEndpoints {
        cfg.unifiedExportConfig?.let { unified ->
            return ExportEndpoints(
                spans = unified.spansEndpoint(),
                logs = unified.logsEndpoint(),
                metrics = unified.metricsEndpoint(),
                protocol = unified.protocol,
            )
        }

        cfg.grpcExportConfig?.let { grpc ->
            return ExportEndpoints(
                spans = grpc.spansEndpoint(),
                logs = grpc.logsEndpoint(),
                metrics = grpc.metricsEndpoint(),
                protocol = ExportProtocol.GRPC,
            )
        }

        return ExportEndpoints(
            spans = cfg.exportConfig.spansEndpoint(),
            logs = cfg.exportConfig.logsEndpoint(),
            metrics = cfg.exportConfig.metricsEndpoint(),
            protocol = ExportProtocol.HTTP,
        )
    }

    private fun createSpanExporter(
        endpoint: EndpointConnectivity,
        protocol: ExportProtocol,
    ): SpanExporter =
        when (protocol) {
            ExportProtocol.HTTP -> {
                OtlpHttpSpanExporter
                    .builder()
                    .setEndpoint(endpoint.getUrl())
                    .setHeaders(endpoint::getHeaders)
                    .setCompression(endpoint.getCompression().getUpstreamName())
                    .build()
            }

            ExportProtocol.GRPC -> {
                OtlpGrpcSpanExporter
                    .builder()
                    .setEndpoint(endpoint.getUrl())
                    .setHeaders(endpoint::getHeaders)
                    .setCompression(endpoint.getCompression().getUpstreamName())
                    .build()
            }
        }

    private fun createLogRecordExporter(
        endpoint: EndpointConnectivity,
        protocol: ExportProtocol,
    ): LogRecordExporter =
        when (protocol) {
            ExportProtocol.HTTP -> {
                OtlpHttpLogRecordExporter
                    .builder()
                    .setEndpoint(endpoint.getUrl())
                    .setHeaders(endpoint::getHeaders)
                    .setCompression(endpoint.getCompression().getUpstreamName())
                    .build()
            }

            ExportProtocol.GRPC -> {
                OtlpGrpcLogRecordExporter
                    .builder()
                    .setEndpoint(endpoint.getUrl())
                    .setHeaders(endpoint::getHeaders)
                    .setCompression(endpoint.getCompression().getUpstreamName())
                    .build()
            }
        }

    private fun createMetricExporter(
        endpoint: EndpointConnectivity,
        protocol: ExportProtocol,
    ): MetricExporter =
        when (protocol) {
            ExportProtocol.HTTP -> {
                OtlpHttpMetricExporter
                    .builder()
                    .setEndpoint(endpoint.getUrl())
                    .setHeaders(endpoint::getHeaders)
                    .setCompression(endpoint.getCompression().getUpstreamName())
                    .build()
            }

            ExportProtocol.GRPC -> {
                OtlpGrpcMetricExporter
                    .builder()
                    .setEndpoint(endpoint.getUrl())
                    .setHeaders(endpoint::getHeaders)
                    .setCompression(endpoint.getCompression().getUpstreamName())
                    .build()
            }
        }

    private fun Compression.getUpstreamName(): String =
        when (this) {
            Compression.GZIP -> "gzip"
            else -> "none"
        }

    private fun createSessionProvider(
        context: Context,
        cfg: OpenTelemetryConfiguration,
    ): SessionProvider {
        val sessionConfig =
            SessionConfig(
                cfg.sessionConfig.backgroundInactivityTimeout,
                cfg.sessionConfig.maxLifetime,
            )
        val clock = cfg.clock
        val timeoutHandler = SessionIdTimeoutHandler(sessionConfig, clock)
        Services.get(context).appLifecycle.registerListener(timeoutHandler)
        return SessionManager.create(timeoutHandler, sessionConfig, clock)
    }
}
