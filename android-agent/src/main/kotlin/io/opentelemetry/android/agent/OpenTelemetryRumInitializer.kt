/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Application
import android.content.Context
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.RumBuilder
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.dsl.OpenTelemetryConfiguration
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.agent.session.SessionManager
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter

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
        val cfg = OpenTelemetryConfiguration()
        configuration(cfg)

        val sessionConfig =
            SessionConfig(
                cfg.sessionConfig.backgroundInactivityTimeout,
                cfg.sessionConfig.maxLifetime,
            )

        // ensure we're using the Application Context to prevent potential leaks.
        // if context.applicationContext is null (e.g. called from within attachBaseContext),
        // fallback to the supplied context.
        val ctx =
            when (context) {
                is Application -> context
                else -> context.applicationContext ?: context
            }

        val spansEndpoint = cfg.exportConfig.spansEndpoint()
        val logsEndpoints = cfg.exportConfig.logsEndpoint()
        val metricsEndpoint = cfg.exportConfig.metricsEndpoint()
        return RumBuilder
            .builder(ctx, cfg.rumConfig)
            .setSessionProvider(createSessionProvider(ctx, sessionConfig))
            .addSpanExporterCustomizer {
                OtlpHttpSpanExporter
                    .builder()
                    .setEndpoint(spansEndpoint.getUrl())
                    .setHeaders(spansEndpoint::getHeaders)
                    .setCompression(spansEndpoint.getCompression().getUpstreamName())
                    .build()
            }.addLogRecordExporterCustomizer {
                OtlpHttpLogRecordExporter
                    .builder()
                    .setEndpoint(logsEndpoints.getUrl())
                    .setHeaders(logsEndpoints::getHeaders)
                    .setCompression(logsEndpoints.getCompression().getUpstreamName())
                    .build()
            }.addMetricExporterCustomizer {
                OtlpHttpMetricExporter
                    .builder()
                    .setEndpoint(metricsEndpoint.getUrl())
                    .setHeaders(metricsEndpoint::getHeaders)
                    .setCompression(metricsEndpoint.getCompression().getUpstreamName())
                    .build()
            }.build()
    }

    private fun Compression.getUpstreamName(): String =
        when (this) {
            Compression.GZIP -> "gzip"
            else -> "none"
        }

    private fun createSessionProvider(
        context: Context,
        sessionConfig: SessionConfig,
    ): SessionProvider {
        val timeoutHandler = SessionIdTimeoutHandler(sessionConfig)
        Services.get(context).appLifecycle.registerListener(timeoutHandler)
        return SessionManager.create(timeoutHandler, sessionConfig)
    }
}
