/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationRegistry
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

fun OtelRumConfig.setActivityTracerCustomizer(customizer: (Tracer) -> Tracer): OtelRumConfig {
    AndroidInstrumentationRegistry.get().get(ActivityLifecycleInstrumentation::class.java)
        ?.setTracerCustomizer(customizer)
    return this
}

fun OtelRumConfig.setActivityNameExtractor(screenNameExtractor: ScreenNameExtractor): OtelRumConfig {
    AndroidInstrumentationRegistry.get().get(ActivityLifecycleInstrumentation::class.java)
        ?.setScreenNameExtractor(screenNameExtractor)
    return this
}

fun OtelRumConfig.setFragmentTracerCustomizer(customizer: (Tracer) -> Tracer): OtelRumConfig {
    AndroidInstrumentationRegistry.get().get(FragmentLifecycleInstrumentation::class.java)
        ?.setTracerCustomizer(customizer)
    return this
}

fun OtelRumConfig.setFragmentNameExtractor(screenNameExtractor: ScreenNameExtractor): OtelRumConfig {
    AndroidInstrumentationRegistry.get().get(FragmentLifecycleInstrumentation::class.java)
        ?.setScreenNameExtractor(screenNameExtractor)
    return this
}

fun OtelRumConfig.addAnrAttributesExtractor(extractor: AttributesExtractor<Array<StackTraceElement>, Void>): OtelRumConfig {
    AndroidInstrumentationRegistry.get().get(AnrInstrumentation::class.java)
        ?.addAttributesExtractor(extractor)
    return this
}

fun OtelRumConfig.addCrashAttributesExtractor(extractor: AttributesExtractor<CrashDetails, Void>): OtelRumConfig {
    AndroidInstrumentationRegistry.get().get(CrashReporterInstrumentation::class.java)
        ?.addAttributesExtractor(extractor)
    return this
}

fun OtelRumConfig.addNetworkChangeAttributesExtractor(extractor: AttributesExtractor<CurrentNetwork, Void>): OtelRumConfig {
    AndroidInstrumentationRegistry.get().get(NetworkChangeInstrumentation::class.java)
        ?.addAttributesExtractor(extractor)
    return this
}

fun OtelRumConfig.setSlowRenderingDetectionPollInterval(interval: Duration): OtelRumConfig {
    AndroidInstrumentationRegistry.get().get(SlowRenderingInstrumentation::class.java)
        ?.setSlowRenderingDetectionPollInterval(interval)
    return this
}
