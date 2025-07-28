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
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.activity.ActivityLifecycleInstrumentation
import io.opentelemetry.android.instrumentation.anr.AnrInstrumentation
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.instrumentation.crash.CrashDetails
import io.opentelemetry.android.instrumentation.crash.CrashReporterInstrumentation
import io.opentelemetry.android.instrumentation.fragment.FragmentLifecycleInstrumentation
import io.opentelemetry.android.instrumentation.network.NetworkAttributesExtractor
import io.opentelemetry.android.instrumentation.network.NetworkChangeInstrumentation
import io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider
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
     * @param sessionConfig The session configuration, which includes inactivity timeout and maximum lifetime durations.
     * @param activityTracerCustomizer Tracer customizer for [ActivityLifecycleInstrumentation].
     * @param activityNameExtractor Name extractor for [ActivityLifecycleInstrumentation].
     * @param fragmentTracerCustomizer Tracer customizer for [FragmentLifecycleInstrumentation].
     * @param fragmentNameExtractor Name extractor for [FragmentLifecycleInstrumentation].
     * @param anrAttributesExtractors Attribute extractors for [AnrInstrumentation].
     * @param crashAttributesExtractors Attribute extractors for [CrashReporterInstrumentation].
     * @param networkChangeAttributesExtractors Attribute extractors for [NetworkChangeInstrumentation].
     * @param slowRenderingDetectionPollInterval Slow rendering detection interval for [SlowRenderingInstrumentation].
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
        sessionConfig: SessionConfig = SessionConfig.withDefaults(),
        activityTracerCustomizer: ((Tracer) -> Tracer)? = null,
        activityNameExtractor: ScreenNameExtractor? = null,
        fragmentTracerCustomizer: ((Tracer) -> Tracer)? = null,
        fragmentNameExtractor: ScreenNameExtractor? = null,
        anrAttributesExtractors: List<EventAttributesExtractor<Array<StackTraceElement>>> = emptyList(),
        crashAttributesExtractors: List<AttributesExtractor<CrashDetails, Void>> = emptyList(),
        networkChangeAttributesExtractors: List<NetworkAttributesExtractor> = emptyList(),
        slowRenderingDetectionPollInterval: Duration? = null,
    ): OpenTelemetryRum {
        configureInstrumentation(
            activityTracerCustomizer,
            activityNameExtractor,
            fragmentTracerCustomizer,
            fragmentNameExtractor,
            anrAttributesExtractors,
            crashAttributesExtractors,
            networkChangeAttributesExtractors,
            slowRenderingDetectionPollInterval,
        )

        return OpenTelemetryRum
            .builder(application, rumConfig)
            .setSessionProvider(createSessionProvider(application, sessionConfig))
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
        application: Application,
        sessionConfig: SessionConfig,
    ): SessionProvider {
        val timeoutHandler = SessionIdTimeoutHandler(sessionConfig)
        Services.get(application).appLifecycle.registerListener(timeoutHandler)
        return SessionManager.create(timeoutHandler, sessionConfig)
    }

    private fun configureInstrumentation(
        activityTracerCustomizer: ((Tracer) -> Tracer)?,
        activityNameExtractor: ScreenNameExtractor?,
        fragmentTracerCustomizer: ((Tracer) -> Tracer)?,
        fragmentNameExtractor: ScreenNameExtractor?,
        anrAttributesExtractors: List<EventAttributesExtractor<Array<StackTraceElement>>>,
        crashAttributesExtractors: List<AttributesExtractor<CrashDetails, Void>>,
        networkChangeAttributesExtractors: List<NetworkAttributesExtractor>,
        slowRenderingDetectionPollInterval: Duration?,
    ) {
        val activityLifecycleInstrumentation =
            getInstrumentation<ActivityLifecycleInstrumentation>()
        if (activityTracerCustomizer != null) {
            activityLifecycleInstrumentation?.setTracerCustomizer(activityTracerCustomizer)
        }
        if (activityNameExtractor != null) {
            activityLifecycleInstrumentation?.setScreenNameExtractor(activityNameExtractor)
        }

        val fragmentLifecycleInstrumentation =
            getInstrumentation<FragmentLifecycleInstrumentation>()
        if (fragmentTracerCustomizer != null) {
            fragmentLifecycleInstrumentation?.setTracerCustomizer(fragmentTracerCustomizer)
        }
        if (fragmentNameExtractor != null) {
            fragmentLifecycleInstrumentation?.setScreenNameExtractor(fragmentNameExtractor)
        }

        if (anrAttributesExtractors.isNotEmpty()) {
            val anrInstrumentation = getInstrumentation<AnrInstrumentation>()
            for (extractor in anrAttributesExtractors) {
                anrInstrumentation?.addAttributesExtractor(extractor)
            }
        }

        if (crashAttributesExtractors.isNotEmpty()) {
            val crashInstrumentation = getInstrumentation<CrashReporterInstrumentation>()
            for (extractor in crashAttributesExtractors) {
                crashInstrumentation?.addAttributesExtractor(extractor)
            }
        }

        if (networkChangeAttributesExtractors.isNotEmpty()) {
            val networkChangeInstrumentation = getInstrumentation<NetworkChangeInstrumentation>()
            for (extractor in networkChangeAttributesExtractors) {
                networkChangeInstrumentation?.addAttributesExtractor(extractor)
            }
        }

        if (slowRenderingDetectionPollInterval != null) {
            getInstrumentation<SlowRenderingInstrumentation>()?.setSlowRenderingDetectionPollInterval(
                slowRenderingDetectionPollInterval,
            )
        }
    }

    private inline fun <reified T : AndroidInstrumentation> getInstrumentation(): T? =
        AndroidInstrumentationLoader.getInstrumentation(T::class.java)
}
