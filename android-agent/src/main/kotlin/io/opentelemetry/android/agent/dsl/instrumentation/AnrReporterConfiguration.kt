/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.anr.AnrInstrumentation
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor

/**
 * Type-safe config DSL that controls how ANR instrumentation should behave.
 */
@OpenTelemetryDslMarker
class AnrReporterConfiguration internal constructor(
    private val config: OtelRumConfig,
) : WithEventAttributes<Array<StackTraceElement>>,
    CanBeEnabledAndDisabled {
    private val anrInstrumentation: AnrInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(
            AnrInstrumentation::class.java,
        )
    }

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
