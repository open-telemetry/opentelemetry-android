/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Application
import android.content.Context
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.OpenTelemetryRumBuilder
import io.opentelemetry.android.RumBuilder
import io.opentelemetry.android.agent.dsl.OpenTelemetryConfiguration
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.agent.session.SessionManager
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.export.FilteringSpanExporter
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.contrib.interceptor.InterceptableLogRecordExporter
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
        var manager: SessionManager? = null
        return RumBuilder
            .builder(ctx, rumConfig)
            .apply {
                val cfg = createConfiguration(rumConfig, instrumentationLoader, configuration)
                val exportConfiguration = cfg.resolveExportConfiguration()
                cfg.diskBufferingConfig.applyToRumConfig()

                val sessionProvider =
                    cfg.sessionConfig.sessionProvider ?: createSessionProvider(Services.get(ctx).appLifecycle, cfg).also { manager = it }
                setSessionProvider(sessionProvider)
                setResource(cfg.resourceProvider(ctx))
                setClock(cfg.clock)

                if (cfg.propagators.isNotEmpty()) {
                    addPropagatorCustomizer { existing ->
                        TextMapPropagator.composite(listOf(existing) + cfg.propagators)
                    }
                }

                configureExporters(this, cfg, rumConfig, sessionProvider, exportConfiguration)
            }.build()
            .let { IncubatingOpenTelemetryRumImpl(it, manager) }
    }

    private fun configureExporters(
        builder: OpenTelemetryRumBuilder,
        cfg: OpenTelemetryConfiguration,
        rumConfig: OtelRumConfig,
        sessionProvider: SessionProvider,
        exportConfiguration: ResolvedExportConfiguration,
    ) {
        if (rumConfig.tracingEnabled) {
            cfg.samplerConfig.traceSamplerFactory?.let { factory ->
                // Resolve user configuration before build starts any SDK workers.
                val sampler = requireNotNull(factory(sessionProvider)) { "Trace sampler factory returned null" }
                builder.registerTracerProviderCustomizer { tracerProviderBuilder, _ -> tracerProviderBuilder.setSampler(sampler) }
            }
            builder.addSpanExporterCustomizer { createSpanExporter(exportConfiguration) }
            HttpSpanHostFilter
                .create(cfg.instrumentations.httpTelemetry.recordSpanForHost())
                ?.let { hostFilter ->
                    builder.addSpanExporterCustomizer { exporter ->
                        FilteringSpanExporter
                            .builder(exporter)
                            .rejecting { span -> hostFilter.rejects(span) }
                            .build()
                    }
                }
        }
        if (rumConfig.loggingEnabled) {
            val websocketEventHostFilter =
                WebsocketEventHostFilter.create(cfg.instrumentations.httpTelemetry.recordWebsocketEventForHost())
            builder.addLogRecordExporterCustomizer {
                applyWebsocketEventHostFilter(
                    OtlpExporterFactory.createLogExporter(exportConfiguration.transport, exportConfiguration.logs),
                    websocketEventHostFilter,
                )
            }
        }
        if (rumConfig.metricsEnabled) {
            builder.addMetricExporterCustomizer { createMetricExporter(exportConfiguration) }
        }
    }

    private fun createSpanExporter(config: ResolvedExportConfiguration): SpanExporter =
        OtlpExporterFactory.createSpanExporter(config.transport, config.spans)

    private fun createMetricExporter(config: ResolvedExportConfiguration): MetricExporter =
        OtlpExporterFactory.createMetricExporter(config.transport, config.metrics)

    private fun applyWebsocketEventHostFilter(
        exporter: LogRecordExporter,
        hostFilter: WebsocketEventHostFilter?,
    ): LogRecordExporter {
        if (hostFilter == null) return exporter
        return InterceptableLogRecordExporter(exporter) { record ->
            if (hostFilter.rejects(record)) null else record
        }
    }

    private fun createConfiguration(
        rumConfig: OtelRumConfig,
        instrumentationLoader: AndroidInstrumentationLoader,
        configuration: OpenTelemetryConfiguration.() -> Unit,
    ): OpenTelemetryConfiguration =
        OpenTelemetryConfiguration(
            rumConfig = rumConfig,
            instrumentationLoader = instrumentationLoader,
        ).also(configuration).also { it.validateExportConfiguration() }

    private fun createSessionProvider(
        appLifecycle: AppLifecycle,
        cfg: OpenTelemetryConfiguration,
    ): SessionManager {
        val sessionConfig =
            SessionConfig(
                cfg.sessionConfig.backgroundInactivityTimeout,
                cfg.sessionConfig.maxLifetime,
            )
        val clock = cfg.clock
        val timeoutHandler = SessionIdTimeoutHandler(sessionConfig, clock)
        appLifecycle.registerListener(timeoutHandler)
        val sessionManager = SessionManager.create(timeoutHandler, sessionConfig, clock, cfg.sessionConfig.sessionStorage)
        cfg.sessionConfig.getObservers().forEach { sessionManager.addObserver(it) }
        return sessionManager
    }
}
