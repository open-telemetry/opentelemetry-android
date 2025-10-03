/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Application
import android.content.Context
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.dsl.DiskBufferingConfigurationSpec
import io.opentelemetry.android.agent.dsl.OpenTelemetryConfiguration
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.agent.session.SessionManager
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter

@OptIn(Incubating::class)
object OpenTelemetryRumInitializer {
    /**
     * Opinionated [OpenTelemetryRum] initialization.
     *
     * @param application Your android app's application object.
     * @param globalAttributes Configures the set of global attributes to emit with every span and event.
     * @param diskBuffering Configures the disk buffering feature.
     * @param configuration Type-safe config DSL that controls how OpenTelemetry
     * should behave.
     */
    @JvmStatic
    fun initialize(
        application: Application,
        globalAttributes: (() -> Attributes)? = null,
        diskBuffering: (DiskBufferingConfigurationSpec.() -> Unit)? = null,
        configuration: (OpenTelemetryConfiguration.() -> Unit) = {},
    ): OpenTelemetryRum =
        initialize(
            context = application,
            globalAttributes = globalAttributes,
            diskBuffering = diskBuffering,
            configuration = configuration,
        )

    /**
     * Opinionated [OpenTelemetryRum] initialization.
     *
     * @param context Your android app's application context. This should be from your Application
     * subclass or an appropriate context that allows retrieving the application context. If you
     * supply an inappropriate context (e.g. from attachBaseContext) then instrumentation relying
     * on activity lifecycle callbacks will not function correctly.
     * @param globalAttributes Configures the set of global attributes to emit with every span and event.
     * @param diskBuffering Configures the disk buffering feature.
     * @param configuration Type-safe config DSL that controls how OpenTelemetry
     * should behave.
     */
    @JvmStatic
    fun initialize(
        context: Context,
        globalAttributes: (() -> Attributes)? = null,
        diskBuffering: (DiskBufferingConfigurationSpec.() -> Unit)? = null,
        configuration: (OpenTelemetryConfiguration.() -> Unit) = {},
    ): OpenTelemetryRum {
        val rumConfig = OtelRumConfig()
        val cfg = OpenTelemetryConfiguration(rumConfig)
        configuration(cfg)

        val diskBufferingConfigurationSpec = DiskBufferingConfigurationSpec()
        diskBuffering?.invoke(diskBufferingConfigurationSpec)
        rumConfig.setDiskBufferingConfig(DiskBufferingConfig.create(enabled = diskBufferingConfigurationSpec.enabled))

        globalAttributes?.let {
            rumConfig.setGlobalAttributes(it::invoke)
        }

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

        return OpenTelemetryRum
            .builder(ctx, rumConfig)
            .setSessionProvider(createSessionProvider(ctx, sessionConfig))
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
        context: Context,
        sessionConfig: SessionConfig,
    ): SessionProvider {
        val timeoutHandler = SessionIdTimeoutHandler(sessionConfig)
        Services.get(context).appLifecycle.registerListener(timeoutHandler)
        return SessionManager.create(timeoutHandler, sessionConfig)
    }
}
