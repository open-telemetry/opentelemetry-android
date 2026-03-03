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

/**
 * Type-safe config DSL that controls how slow render event instrumentation should behave.
 */
@OpenTelemetryDslMarker
class SlowRenderingReporterConfiguration internal constructor(
    private val config: OtelRumConfig,
) : CanBeEnabledAndDisabled {
    private val slowRenderingInstrumentation: SlowRenderingInstrumentation? by lazy {
        AndroidInstrumentationLoader.get().getByType(
            SlowRenderingInstrumentation::class.java,
        )
    }

    /**
     * Sets the poll interval for slow rendering detection.
     */
    fun detectionPollInterval(value: Duration) {
        slowRenderingInstrumentation?.setSlowRenderingDetectionPollInterval(value.toJavaDuration())
    }

    /**
     * Enables verbose debug logging for slow rendering instrumentation.
     */
    fun enableVerboseDebugLogging() {
        slowRenderingInstrumentation?.enableVerboseDebugLogging()
    }

    override fun enabled(enabled: Boolean) {
        if (enabled) {
            slowRenderingInstrumentation?.name?.let { config.allowInstrumentation(it) }
        } else {
            slowRenderingInstrumentation?.name?.let { config.suppressInstrumentation(it) }
        }
    }
}
