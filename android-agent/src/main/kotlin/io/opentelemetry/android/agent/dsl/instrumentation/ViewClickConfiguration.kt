package io.opentelemetry.android.agent.dsl.instrumentation

import android.util.Log
import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.view.click.ViewClickInstrumentation

@OpenTelemetryDslMarker
class ViewClickConfiguration internal constructor(
    private val config: OtelRumConfig,
    private val instrumentationLoader: AndroidInstrumentationLoader,
): CanBeEnabledAndDisabled {

    private val viewClickInstrumentation: ViewClickInstrumentation? by lazy {
        instrumentationLoader.getByType(ViewClickInstrumentation::class.java)
    }

    override fun enabled(enabled: Boolean) {
        if (enabled) {
            viewClickInstrumentation?.name?.let { config.allowInstrumentation(it) }
        } else {
            viewClickInstrumentation?.name?.let { config.suppressInstrumentation(it) }
        }
    }
}
