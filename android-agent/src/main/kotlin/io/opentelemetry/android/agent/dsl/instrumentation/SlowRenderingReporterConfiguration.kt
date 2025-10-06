/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@OpenTelemetryDslMarker
class SlowRenderingReporterConfiguration internal constructor(
    private val config: OtelRumConfig,
) : CanBeEnabledAndDisabled {
    private val slowRenderingInstrumentation: SlowRenderingInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(
            SlowRenderingInstrumentation::class.java,
        )!!
    }

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
