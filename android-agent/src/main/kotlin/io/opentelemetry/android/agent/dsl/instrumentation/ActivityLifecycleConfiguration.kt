/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.activity.ActivityLifecycleInstrumentation
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.api.trace.Tracer

/**
 * Type-safe config DSL that controls how activity lifecycle instrumentation should behave.
 */
@OpenTelemetryDslMarker
class ActivityLifecycleConfiguration internal constructor(
    private val config: OtelRumConfig,
) : ScreenLifecycleConfigurable,
    CanBeEnabledAndDisabled {
    private val activityLifecycleInstrumentation: ActivityLifecycleInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(ActivityLifecycleInstrumentation::class.java)
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
