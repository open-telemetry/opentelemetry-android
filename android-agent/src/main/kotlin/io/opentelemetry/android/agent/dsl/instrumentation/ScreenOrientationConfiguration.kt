/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.screenorientation.ScreenOrientationInstrumentation

@OpenTelemetryDslMarker
class ScreenOrientationConfiguration internal constructor(
    private val config: OtelRumConfig,
) : CanBeEnabledAndDisabled {
    private val instrumentation: ScreenOrientationInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(
            ScreenOrientationInstrumentation::class.java,
        )
    }

    override fun enabled(enabled: Boolean) {
        if (enabled) {
            config.allowInstrumentation(instrumentation.name)
        } else {
            config.suppressInstrumentation(instrumentation.name)
        }
    }
}
