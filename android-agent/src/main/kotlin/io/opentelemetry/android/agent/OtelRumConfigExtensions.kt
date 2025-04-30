/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.activity.ActivityLifecycleInstrumentation
import io.opentelemetry.android.instrumentation.anr.AnrInstrumentation
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.instrumentation.crash.CrashDetails
import io.opentelemetry.android.instrumentation.crash.CrashReporterInstrumentation
import io.opentelemetry.android.instrumentation.fragment.FragmentLifecycleInstrumentation
import io.opentelemetry.android.instrumentation.network.NetworkAttributesExtractor
import io.opentelemetry.android.instrumentation.network.NetworkChangeInstrumentation
import io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation
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
    AndroidInstrumentationLoader
        .getInstrumentation(ActivityLifecycleInstrumentation::class.java)
        ?.setTracerCustomizer(customizer)
    return this
}

fun OtelRumConfig.setActivityNameExtractor(screenNameExtractor: ScreenNameExtractor): OtelRumConfig {
    AndroidInstrumentationLoader
        .getInstrumentation(ActivityLifecycleInstrumentation::class.java)
        ?.setScreenNameExtractor(screenNameExtractor)
    return this
}

fun OtelRumConfig.setFragmentTracerCustomizer(customizer: (Tracer) -> Tracer): OtelRumConfig {
    AndroidInstrumentationLoader
        .getInstrumentation(FragmentLifecycleInstrumentation::class.java)
        ?.setTracerCustomizer(customizer)
    return this
}

fun OtelRumConfig.setFragmentNameExtractor(screenNameExtractor: ScreenNameExtractor): OtelRumConfig {
    AndroidInstrumentationLoader
        .getInstrumentation(FragmentLifecycleInstrumentation::class.java)
        ?.setScreenNameExtractor(screenNameExtractor)
    return this
}

fun OtelRumConfig.addAnrAttributesExtractor(extractor: AttributesExtractor<Array<StackTraceElement>, Void>): OtelRumConfig {
    AndroidInstrumentationLoader
        .getInstrumentation(AnrInstrumentation::class.java)
        ?.addAttributesExtractor(extractor)
    return this
}

fun OtelRumConfig.addCrashAttributesExtractor(extractor: AttributesExtractor<CrashDetails, Void>): OtelRumConfig {
    AndroidInstrumentationLoader
        .getInstrumentation(CrashReporterInstrumentation::class.java)
        ?.addAttributesExtractor(extractor)
    return this
}

fun OtelRumConfig.addNetworkChangeAttributesExtractor(attributeExtractor: NetworkAttributesExtractor): OtelRumConfig {
    AndroidInstrumentationLoader
        .getInstrumentation(NetworkChangeInstrumentation::class.java)
        ?.addAttributesExtractor(attributeExtractor)
    return this
}

fun OtelRumConfig.setSlowRenderingDetectionPollInterval(interval: Duration): OtelRumConfig {
    AndroidInstrumentationLoader
        .getInstrumentation(SlowRenderingInstrumentation::class.java)
        ?.setSlowRenderingDetectionPollInterval(interval)
    return this
}
