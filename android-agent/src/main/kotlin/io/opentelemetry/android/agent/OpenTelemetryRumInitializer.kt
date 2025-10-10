/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Application
import android.content.Context
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.connectivity.EndpointConnectivity
import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity
import io.opentelemetry.android.agent.dsl.DiskBufferingConfigurationSpec
import io.opentelemetry.android.agent.dsl.instrumentation.InstrumentationConfiguration
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
     * @param endpointBaseUrl The base endpoint for exporting all your signals.
     * @param endpointHeaders These will be added to each signal export request.
     * @param spanEndpointConnectivity Span-specific endpoint configuration.
     * @param logEndpointConnectivity Log-specific endpoint configuration.
     * @param metricEndpointConnectivity Metric-specific endpoint configuration.
     * @param sessionConfig The session configuration, which includes inactivity timeout and maximum lifetime durations.
     * @param instrumentations Configurations for all the default instrumentations.
     */
    @Suppress("LongParameterList")
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
        sessionConfig: SessionConfig = SessionConfig.withDefaults(),
        globalAttributes: (() -> Attributes)? = null,
        diskBuffering: (DiskBufferingConfigurationSpec.() -> Unit)? = null,
        instrumentations: (InstrumentationConfiguration.() -> Unit)? = null,
    ): OpenTelemetryRum =
        initialize(
            context = application,
            endpointBaseUrl = endpointBaseUrl,
            endpointHeaders = endpointHeaders,
            spanEndpointConnectivity = spanEndpointConnectivity,
            logEndpointConnectivity = logEndpointConnectivity,
            metricEndpointConnectivity = metricEndpointConnectivity,
            globalAttributes = globalAttributes,
            diskBuffering = diskBuffering,
            sessionConfig = sessionConfig,
            instrumentations = instrumentations,
        )

    /**
     * Opinionated [OpenTelemetryRum] initialization.
     *
     * @param context Your android app's application context. This should be from your Application
     * subclass or an appropriate context that allows retrieving the application context. If you
     * supply an inappropriate context (e.g. from attachBaseContext) then instrumentation relying
     * on activity lifecycle callbacks will not function correctly.
     * @param endpointBaseUrl The base endpoint for exporting all your signals.
     * @param endpointHeaders These will be added to each signal export request.
     * @param spanEndpointConnectivity Span-specific endpoint configuration.
     * @param logEndpointConnectivity Log-specific endpoint configuration.
     * @param metricEndpointConnectivity Metric-specific endpoint configuration.
     * @param sessionConfig The session configuration, which includes inactivity timeout and maximum lifetime durations.
     * @param instrumentations Configurations for all the default instrumentations.
     */
    @Suppress("LongParameterList")
    @JvmStatic
    fun initialize(
        context: Context,
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
        sessionConfig: SessionConfig = SessionConfig.withDefaults(),
        globalAttributes: (() -> Attributes)? = null,
        diskBuffering: (DiskBufferingConfigurationSpec.() -> Unit)? = null,
        instrumentations: (InstrumentationConfiguration.() -> Unit)? = null,
    ): OpenTelemetryRum {
        val rumConfig = OtelRumConfig()

        instrumentations?.let { configure ->
            InstrumentationConfiguration(rumConfig).configure()
        }

        val diskBufferingConfigurationSpec = DiskBufferingConfigurationSpec()
        diskBuffering?.invoke(diskBufferingConfigurationSpec)
        rumConfig.setDiskBufferingConfig(DiskBufferingConfig.create(enabled = diskBufferingConfigurationSpec.enabled))

        globalAttributes?.let {
            rumConfig.setGlobalAttributes(it::invoke)
        }

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

    private fun createSessionProvider(
        context: Context,
        sessionConfig: SessionConfig,
    ): SessionProvider {
        val timeoutHandler = SessionIdTimeoutHandler(sessionConfig)
        Services.get(context).appLifecycle.registerListener(timeoutHandler)
        return SessionManager.create(timeoutHandler, sessionConfig)
    }
}
