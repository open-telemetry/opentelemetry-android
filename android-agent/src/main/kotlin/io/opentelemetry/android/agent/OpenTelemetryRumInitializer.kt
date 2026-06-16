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
import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity
import io.opentelemetry.android.agent.dsl.OpenTelemetryConfiguration
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.agent.session.SessionManager
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.trace.export.SpanExporter

@OptIn(Incubating::class)
object OpenTelemetryRumInitializer {
    /**
     * Opinionated [io.opentelemetry.android.OpenTelemetryRum] initialization.
     *
     * @param context Your android app's application context. This should be from your Application
     * subclass or an appropriate context that allows retrieving the application context. If you
     * supply an inappropriate context (e.g. from attachBaseContext) then instrumentation relying
     * on activity lifecycle callbacks will not function correctly.
     * @param configuration Type-safe config DSL that controls how OpenTelemetry
     * should behave.
     */
    @JvmStatic
    fun initialize(
        context: Context,
        configuration: (OpenTelemetryConfiguration.() -> Unit) = {},
    ): OpenTelemetryRum {
        // ensure we're using the Application Context to prevent potential leaks.
        // if context.applicationContext is null (e.g. called from within attachBaseContext),
        // fallback to the supplied context.
        val ctx =
            when (context) {
                is Application -> context
                else -> context.applicationContext ?: context
            }

        val rumConfig = OtelRumConfig()
        return RumBuilder.builder(ctx, rumConfig).apply {
            val cfg = OpenTelemetryConfiguration(
                rumConfig = rumConfig,
                instrumentationLoader = instrumentationLoader
            ).also(configuration)

            setSessionProvider(createSessionProvider(Services.get(ctx).appLifecycle, cfg))
            setResource(
                AndroidResource.createDefault(ctx).toBuilder().apply {
                    cfg.resourceAction(this)
                }.build()
            )
            setClock(cfg.clock)

            if (rumConfig.tracingEnabled) {
                addSpanExporterCustomizer {
                    createSpanExporter(cfg.exportConfig.spansEndpoint())
                }
            }
            if (rumConfig.loggingEnabled) {
                addLogRecordExporterCustomizer {
                    createLogExporter(cfg.exportConfig.logsEndpoint())
                }
            }
            if (rumConfig.metricsEnabled) {
                addMetricExporterCustomizer {
                    createMetricExporter(cfg.exportConfig.metricsEndpoint())
                }
            }
        }.build()
    }

    private fun createSpanExporter(endpoint: HttpEndpointConnectivity): SpanExporter =
        OtlpHttpSpanExporter
            .builder()
            .setEndpoint(endpoint.getUrl())
            .setHeaders(endpoint::getHeaders)
            .setCompression(endpoint.getCompression().getUpstreamName())
            .apply {
                endpoint.getClientTls()?.let {
                    setClientTls(it.privateKeyPem, it.certificatePem)
                }
                endpoint.getSslContext()?.let {
                    setSslContext(it.sslContext, it.sslX509TrustManager)
                }
            }.build()

    private fun createLogExporter(endpoint: HttpEndpointConnectivity): LogRecordExporter =
        OtlpHttpLogRecordExporter
            .builder()
            .setEndpoint(endpoint.getUrl())
            .setHeaders(endpoint::getHeaders)
            .setCompression(endpoint.getCompression().getUpstreamName())
            .apply {
                endpoint.getClientTls()?.let {
                    setClientTls(it.privateKeyPem, it.certificatePem)
                }
                endpoint.getSslContext()?.let {
                    setSslContext(it.sslContext, it.sslX509TrustManager)
                }
            }.build()

    private fun createMetricExporter(endpoint: HttpEndpointConnectivity): MetricExporter =
        OtlpHttpMetricExporter
            .builder()
            .setEndpoint(endpoint.getUrl())
            .setHeaders(endpoint::getHeaders)
            .setCompression(endpoint.getCompression().getUpstreamName())
            .apply {
                endpoint.getClientTls()?.let {
                    setClientTls(it.privateKeyPem, it.certificatePem)
                }
                endpoint.getSslContext()?.let {
                    setSslContext(it.sslContext, it.sslX509TrustManager)
                }
            }.build()

    private fun Compression.getUpstreamName(): String =
        when (this) {
            Compression.GZIP -> "gzip"
            else -> "none"
        }

    private fun createSessionProvider(
        appLifecycle: AppLifecycle,
        cfg: OpenTelemetryConfiguration,
    ): SessionProvider {
        val sessionConfig =
            SessionConfig(
                cfg.sessionConfig.backgroundInactivityTimeout,
                cfg.sessionConfig.maxLifetime,
            )
        val clock = cfg.clock
        val timeoutHandler = SessionIdTimeoutHandler(sessionConfig, clock)
        appLifecycle.registerListener(timeoutHandler)
        val sessionManager = SessionManager.create(timeoutHandler, sessionConfig, clock)
        cfg.sessionConfig.getObservers().forEach { sessionManager.addObserver(it) }
        return sessionManager
    }
}
