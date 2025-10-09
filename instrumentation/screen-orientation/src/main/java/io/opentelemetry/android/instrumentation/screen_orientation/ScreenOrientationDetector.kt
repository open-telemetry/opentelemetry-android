package io.opentelemetry.android.instrumentation.screen_orientation

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.instrumentation.screen_orientation.model.Orientation
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger

internal class ScreenOrientationDetector(
    applicationContext: Context,
    private val logger: Logger,
    private val additionalExtractors: List<EventAttributesExtractor<Orientation>> = emptyList(),
) : ComponentCallbacks {
    private var currentOrientation: Int = applicationContext.resources.configuration.orientation

    private fun emitLog(orientation: Orientation, body: String) {
        val attributesBuilder = Attributes.builder()
        additionalExtractors.forEach {
            it.extract(io.opentelemetry.context.Context.current(), orientation).also { attributes ->
                attributesBuilder.putAll(attributes)
            }
        }

        logger.logRecordBuilder()
            .setEventName("device.screen_orientation")
            .setBody(body)
            .setAllAttributes(attributesBuilder.build())
            .emit()
    }

    private val Int.name: String
        get() {
            return when (this) {
                ORIENTATION_LANDSCAPE -> "landscape"
                ORIENTATION_PORTRAIT -> "portrait"
                else -> "Undefined"
            }
        }

    override fun onConfigurationChanged(config: Configuration) {
        if (config.orientation != currentOrientation) {
            currentOrientation = config.orientation
            emitLog(
                Orientation(config.orientation),
                "Screen orientation changed to ${config.orientation.name}."
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        // no op
    }
}
