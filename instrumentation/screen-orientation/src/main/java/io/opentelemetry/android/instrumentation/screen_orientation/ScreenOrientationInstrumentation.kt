package io.opentelemetry.android.instrumentation.screen_orientation

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.instrumentation.screen_orientation.model.Orientation

@AutoService(AndroidInstrumentation::class)
class ScreenOrientationInstrumentation : AndroidInstrumentation {
    private lateinit var detector: ScreenOrientationDetector

    private val additionalExtractors: MutableList<EventAttributesExtractor<Orientation>> =
        mutableListOf()

    fun addAttributesExtractor(extractor: EventAttributesExtractor<Orientation>): ScreenOrientationInstrumentation {
        additionalExtractors.add(extractor)
        return this
    }

    override fun install(ctx: InstallationContext) {
        val logger =
            ctx.openTelemetry
                .logsBridge
                .loggerBuilder("io.opentelemetry.$name")
                .build()
        val applicationContext = ctx.application.applicationContext
        detector = ScreenOrientationDetector(applicationContext, logger, additionalExtractors)
        applicationContext.registerComponentCallbacks(detector)
    }

    override fun uninstall(ctx: InstallationContext) {
        if (!::detector.isInitialized) return

        ctx.application.applicationContext.unregisterComponentCallbacks(detector)
    }

    override val name: String = "screen_orientation"
}
