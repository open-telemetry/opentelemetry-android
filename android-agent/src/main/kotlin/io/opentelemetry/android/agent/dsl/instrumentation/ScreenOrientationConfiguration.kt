package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.instrumentation.screenorientation.ScreenOrientationInstrumentation
import io.opentelemetry.android.instrumentation.screenorientation.model.Orientation

@OpenTelemetryDslMarker
class ScreenOrientationConfiguration internal constructor(
    private val config: OtelRumConfig
) : WithEventAttributes<Orientation>, CanBeEnabledAndDisabled {
    private val instrumentation: ScreenOrientationInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(
            ScreenOrientationInstrumentation::class.java,
        )
    }

    override fun addAttributesExtractor(value: EventAttributesExtractor<Orientation>) {
        instrumentation.addAttributesExtractor(value)
    }

    override fun enabled(enabled: Boolean) {
        if (enabled) {
            config.allowInstrumentation(instrumentation.name)
        } else {
            config.suppressInstrumentation(instrumentation.name)
        }
    }
}
