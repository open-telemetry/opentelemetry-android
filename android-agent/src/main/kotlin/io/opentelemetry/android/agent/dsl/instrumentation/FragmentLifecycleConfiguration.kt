/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.instrumentation.fragment.FragmentLifecycleInstrumentation
import io.opentelemetry.api.trace.Tracer

@OpenTelemetryDslMarker
class FragmentLifecycleConfiguration internal constructor(
    private val config: OtelRumConfig,
) : ScreenLifecycleConfigurable,
    CanBeEnabledAndDisabled {
    private val fragmentLifecycleInstrumentation: FragmentLifecycleInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(FragmentLifecycleInstrumentation::class.java)!!
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
