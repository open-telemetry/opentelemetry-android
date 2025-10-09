/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Application
import io.opentelemetry.android.Incubating
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
import io.opentelemetry.android.instrumentation.screen_orientation.ScreenOrientationInstrumentation
import io.opentelemetry.android.instrumentation.screen_orientation.model.Orientation
import io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@OptIn(Incubating::class)
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
        rumConfig: OtelRumConfig = OtelRumConfig(),
        sessionConfig: SessionConfig = SessionConfig.withDefaults(),
        instrumentations: (InstrumentationConfiguration.() -> Unit)? = null,
    ): OpenTelemetryRum {
        instrumentations?.let { configure ->
            InstrumentationConfiguration(rumConfig).configure()
        }
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

    @InstrumentationConfigMarker
    class InstrumentationConfiguration internal constructor(
        config: OtelRumConfig,
    ) {
        private val activity: ActivityLifecycleConfiguration by lazy {
            ActivityLifecycleConfiguration(
                config,
            )
        }
        private val fragment: FragmentLifecycleConfiguration by lazy {
            FragmentLifecycleConfiguration(
                config,
            )
        }
        private val anr: AnrReporterConfiguration by lazy { AnrReporterConfiguration(config) }
        private val crash: CrashReporterConfiguration by lazy { CrashReporterConfiguration(config) }
        private val networkMonitoring: NetworkMonitoringConfiguration by lazy {
            NetworkMonitoringConfiguration(
                config,
            )
        }
        private val slowRendering: SlowRenderingReporterConfiguration by lazy {
            SlowRenderingReporterConfiguration(
                config,
            )
        }
        private val screenOrientation: ScreenOrientationConfiguration by lazy {
            ScreenOrientationConfiguration(
                config
            )
        }

        fun activity(configure: ActivityLifecycleConfiguration.() -> Unit) {
            activity.configure()
        }

        fun fragment(configure: FragmentLifecycleConfiguration.() -> Unit) {
            fragment.configure()
        }

        fun anrReporter(configure: AnrReporterConfiguration.() -> Unit) {
            anr.configure()
        }

        fun crashReporter(configure: CrashReporterConfiguration.() -> Unit) {
            crash.configure()
        }

        fun networkMonitoring(configure: NetworkMonitoringConfiguration.() -> Unit) {
            networkMonitoring.configure()
        }

        fun slowRenderingReporter(configure: SlowRenderingReporterConfiguration.() -> Unit) {
            slowRendering.configure()
        }

        fun screenOrientation(configure: ScreenOrientationConfiguration.() -> Unit) {
            screenOrientation.configure()
        }
    }

    @InstrumentationConfigMarker
    class ActivityLifecycleConfiguration internal constructor(
        private val config: OtelRumConfig,
    ) : ScreenLifecycleConfigurable,
        CanBeEnabledAndDisabled {
        private val activityLifecycleInstrumentation: ActivityLifecycleInstrumentation by lazy {
            getInstrumentation()
        }

        override fun tracerCustomizer(value: (Tracer) -> Tracer) {
            activityLifecycleInstrumentation.setTracerCustomizer(value)
        }

        override fun screenNameExtractor(value: ScreenNameExtractor) {
            activityLifecycleInstrumentation.setScreenNameExtractor(value)
        }

        override fun enabled(enabled: Boolean) {
            if (enabled) {
                config.allowInstrumentation(activityLifecycleInstrumentation.name)
            } else {
                config.suppressInstrumentation(activityLifecycleInstrumentation.name)
            }
        }
    }

    @InstrumentationConfigMarker
    class FragmentLifecycleConfiguration internal constructor(
        private val config: OtelRumConfig,
    ) : ScreenLifecycleConfigurable,
        CanBeEnabledAndDisabled {
        private val fragmentLifecycleInstrumentation: FragmentLifecycleInstrumentation by lazy {
            getInstrumentation()
        }

        override fun tracerCustomizer(value: (Tracer) -> Tracer) {
            fragmentLifecycleInstrumentation.setTracerCustomizer(value)
        }

        override fun screenNameExtractor(value: ScreenNameExtractor) {
            fragmentLifecycleInstrumentation.setScreenNameExtractor(value)
        }

        override fun enabled(enabled: Boolean) {
            if (enabled) {
                config.allowInstrumentation(fragmentLifecycleInstrumentation.name)
            } else {
                config.suppressInstrumentation(fragmentLifecycleInstrumentation.name)
            }
        }
    }

    @InstrumentationConfigMarker
    class AnrReporterConfiguration internal constructor(
        private val config: OtelRumConfig,
    ) : WithEventAttributes<Array<StackTraceElement>>,
        CanBeEnabledAndDisabled {
        private val anrInstrumentation: AnrInstrumentation by lazy { getInstrumentation() }

        override fun addAttributesExtractor(value: EventAttributesExtractor<Array<StackTraceElement>>) {
            anrInstrumentation.addAttributesExtractor(value)
        }

        override fun enabled(enabled: Boolean) {
            if (enabled) {
                config.allowInstrumentation(anrInstrumentation.name)
            } else {
                config.suppressInstrumentation(anrInstrumentation.name)
            }
        }
    }

    @InstrumentationConfigMarker
    class CrashReporterConfiguration internal constructor(
        private val config: OtelRumConfig,
    ) : WithEventAttributes<CrashDetails>,
        CanBeEnabledAndDisabled {
        private val crashReporterInstrumentation: CrashReporterInstrumentation by lazy { getInstrumentation() }

        override fun addAttributesExtractor(value: EventAttributesExtractor<CrashDetails>) {
            crashReporterInstrumentation.addAttributesExtractor(value)
        }

        override fun enabled(enabled: Boolean) {
            if (enabled) {
                config.allowInstrumentation(crashReporterInstrumentation.name)
            } else {
                config.suppressInstrumentation(crashReporterInstrumentation.name)
            }
        }
    }

    @InstrumentationConfigMarker
    class NetworkMonitoringConfiguration internal constructor(
        private val config: OtelRumConfig,
    ) : CanBeEnabledAndDisabled {
        private val networkInstrumentation: NetworkChangeInstrumentation by lazy { getInstrumentation() }

        fun addAttributesExtractor(value: NetworkAttributesExtractor) {
            networkInstrumentation.addAttributesExtractor(value)
        }

        override fun enabled(enabled: Boolean) {
            if (enabled) {
                config.allowInstrumentation(networkInstrumentation.name)
            } else {
                config.suppressInstrumentation(networkInstrumentation.name)
            }
        }
    }

    @InstrumentationConfigMarker
    class SlowRenderingReporterConfiguration internal constructor(
        private val config: OtelRumConfig,
    ) : CanBeEnabledAndDisabled {
        private val slowRenderingInstrumentation: SlowRenderingInstrumentation by lazy { getInstrumentation() }

        fun detectionPollInterval(value: Duration) {
            slowRenderingInstrumentation.setSlowRenderingDetectionPollInterval(value.toJavaDuration())
        }

        fun enableVerboseDebugLogging() {
            slowRenderingInstrumentation.enableVerboseDebugLogging()
        }

        override fun enabled(enabled: Boolean) {
            if (enabled) {
                config.allowInstrumentation(slowRenderingInstrumentation.name)
            } else {
                config.suppressInstrumentation(slowRenderingInstrumentation.name)
            }
        }
    }

    @InstrumentationConfigMarker
    class ScreenOrientationConfiguration internal constructor(
        private val config: OtelRumConfig
    ) : WithEventAttributes<Orientation>, CanBeEnabledAndDisabled {
        private val instrumentation: ScreenOrientationInstrumentation by lazy { getInstrumentation() }

        override fun addAttributesExtractor(value: EventAttributesExtractor<Orientation>) {
            instrumentation.addAttributesExtractor(value)
        }

        override fun enabled(enabled: Boolean) {
            if (enabled) {
                config.allowInstrumentation(instrumentation.name)
            } else {
                config.suppressInstrumentation(instrumentation.name)
            }
        }
    }

    internal interface ScreenLifecycleConfigurable {
        fun tracerCustomizer(value: (Tracer) -> Tracer)

        fun screenNameExtractor(value: ScreenNameExtractor)
    }

    internal interface WithEventAttributes<T> {
        fun addAttributesExtractor(value: EventAttributesExtractor<T>)
    }

    internal interface CanBeEnabledAndDisabled {
        fun enabled(enabled: Boolean)
    }

    @DslMarker
    internal annotation class InstrumentationConfigMarker

    private inline fun <reified T : AndroidInstrumentation> getInstrumentation(): T =
        AndroidInstrumentationLoader.getInstrumentation(T::class.java)!!
}
