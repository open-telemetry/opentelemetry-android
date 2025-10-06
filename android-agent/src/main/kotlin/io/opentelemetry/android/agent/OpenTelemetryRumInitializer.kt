/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Application
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.dsl.OpenTelemetryConfiguration
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.agent.session.SessionManager
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import kotlin.time.Duration.Companion.minutes

@OptIn(Incubating::class)
object OpenTelemetryRumInitializer {
    /**
     * Opinionated [OpenTelemetryRum] initialization.
     *
     * @param application Your android app's application object.
     * @param openTelemetryConfiguration Type-safe config DSL that controls how OpenTelemetry
     * should behave.
     */
    @Suppress("LongParameterList")
    @JvmStatic
    fun initialize(
        application: Application,
        openTelemetryConfiguration: (OpenTelemetryConfiguration.() -> Unit) = {},
    ): OpenTelemetryRum {
        val cfg = OpenTelemetryConfiguration()
        openTelemetryConfiguration(cfg)

        val sessionConfig =
            SessionConfig(
                cfg.sessionConfig.backgroundInactivityTimeout,
                cfg.sessionConfig.maxLifetime,
            )
        return OpenTelemetryRum
            .builder(application, cfg.rumConfig)
            .setSessionProvider(createSessionProvider(application, sessionConfig))
            .addSpanExporterCustomizer {
                OtlpHttpSpanExporter
                    .builder()
                    .setEndpoint(cfg.exportConfig.spansConfig.url)
                    .setHeaders(cfg.exportConfig.spansConfig::headers)
                    .build()
            }.addLogRecordExporterCustomizer {
                OtlpHttpLogRecordExporter
                    .builder()
                    .setEndpoint(cfg.exportConfig.logsConfig.url)
                    .setHeaders(cfg.exportConfig.logsConfig::headers)
                    .build()
            }.addMetricExporterCustomizer {
                OtlpHttpMetricExporter
                    .builder()
                    .setEndpoint(cfg.exportConfig.metricsConfig.url)
                    .setHeaders(cfg.exportConfig.metricsConfig::headers)
                    .build()
            }.build()
    }

    private fun createSessionProvider(
        application: Application,
        sessionConfig: SessionConfig,
    ): SessionProvider {
        val timeoutHandler = SessionIdTimeoutHandler(sessionConfig)
        Services.get(application).appLifecycle.registerListener(timeoutHandler)
        return SessionManager.create(timeoutHandler, sessionConfig)
    }
}
