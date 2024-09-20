/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.OpenTelemetryRumBuilder
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.activity.ActivityLifecycleInstrumentation
import io.opentelemetry.android.instrumentation.anr.AnrInstrumentation
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.instrumentation.crash.CrashDetails
import io.opentelemetry.android.instrumentation.crash.CrashReporterInstrumentation
import io.opentelemetry.android.instrumentation.fragment.FragmentLifecycleInstrumentation
import io.opentelemetry.android.instrumentation.network.NetworkChangeInstrumentation
import io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import java.time.Duration

/**
 * Convenience functions to allow configuring the default instrumentations through the [OtelRumConfig] object, for example:
 *
 * ```
 * OtelRumConfig()
 *  .setSessionTimeout(Duration.ofSeconds(10)) // Real OtelRumConfig function
 *  .setSlowRenderingDetectionPollInterval(Duration.ofSeconds(5)) // Extension function
 *  .disableScreenAttributes() // Real OtelRumConfig function
 * ```
 */
object AndroidAgent {
    private val activityLifecycleInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(
            ActivityLifecycleInstrumentation::class.java,
        )!!
    }
    private val fragmentLifecycleInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(
            FragmentLifecycleInstrumentation::class.java,
        )!!
    }
    private val anrInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(
            AnrInstrumentation::class.java,
        )!!
    }
    private val crashReporterInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(
            CrashReporterInstrumentation::class.java,
        )!!
    }
    private val networkChangeInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(NetworkChangeInstrumentation::class.java)!!
    }
    private val slowRenderingInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(SlowRenderingInstrumentation::class.java)!!
    }

    fun createRumBuilder(
        application: Application,
        otelRumConfig: OtelRumConfig = OtelRumConfig(),
        activityTracerCustomizer: ((Tracer) -> Tracer)? = null,
        activityNameExtractor: ScreenNameExtractor? = null,
        fragmentTracerCustomizer: ((Tracer) -> Tracer)? = null,
        fragmentNameExtractor: ScreenNameExtractor? = null,
        anrAttributesExtractor: AttributesExtractor<Array<StackTraceElement>, Void>? = null,
        crashAttributesExtractor: AttributesExtractor<CrashDetails, Void>? = null,
        networkChangeAttributesExtractor: AttributesExtractor<CurrentNetwork, Void>? = null,
        slowRenderingDetectionPollInterval: Duration? = null,
    ): OpenTelemetryRumBuilder {
        val rumBuilder = OpenTelemetryRum.builder(application, otelRumConfig)

        applyInstrumentationConfigs(
            activityTracerCustomizer,
            activityNameExtractor,
            fragmentTracerCustomizer,
            fragmentNameExtractor,
            anrAttributesExtractor,
            crashAttributesExtractor,
            networkChangeAttributesExtractor,
            slowRenderingDetectionPollInterval,
        )

        return rumBuilder
    }

    private fun applyInstrumentationConfigs(
        activityTracerCustomizer: ((Tracer) -> Tracer)?,
        activityNameExtractor: ScreenNameExtractor?,
        fragmentTracerCustomizer: ((Tracer) -> Tracer)?,
        fragmentNameExtractor: ScreenNameExtractor?,
        anrAttributesExtractor: AttributesExtractor<Array<StackTraceElement>, Void>?,
        crashAttributesExtractor: AttributesExtractor<CrashDetails, Void>?,
        networkChangeAttributesExtractor: AttributesExtractor<CurrentNetwork, Void>?,
        slowRenderingDetectionPollInterval: Duration?,
    ) {
        activityTracerCustomizer?.let { activityLifecycleInstrumentation.setTracerCustomizer(it) }
        activityNameExtractor?.let { activityLifecycleInstrumentation.setScreenNameExtractor(it) }
        fragmentTracerCustomizer?.let { fragmentLifecycleInstrumentation.setTracerCustomizer(it) }
        fragmentNameExtractor?.let { fragmentLifecycleInstrumentation.setScreenNameExtractor(it) }
        anrAttributesExtractor?.let { anrInstrumentation.addAttributesExtractor(it) }
        crashAttributesExtractor?.let { crashReporterInstrumentation.addAttributesExtractor(it) }
        networkChangeAttributesExtractor?.let {
            networkChangeInstrumentation.addAttributesExtractor(
                it,
            )
        }
        slowRenderingDetectionPollInterval?.let {
            slowRenderingInstrumentation.setSlowRenderingDetectionPollInterval(
                it,
            )
        }
    }
}
