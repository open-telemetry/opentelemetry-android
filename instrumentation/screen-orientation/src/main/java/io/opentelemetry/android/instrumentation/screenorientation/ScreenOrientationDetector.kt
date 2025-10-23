/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.screenorientation

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.instrumentation.screenorientation.model.Orientation
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger

/**
 * Detects and logs screen orientation changes in the Android application.
 *
 * This detector registers as a [ComponentCallbacks] to listen for configuration changes
 * related to screen orientation (portrait, landscape). When a change occurs, it emits
 * a log event via the provided [Logger].
 *
 * Additional metadata can be extracted and attached to logs using [EventAttributesExtractor] instances.
 *
 * @param applicationContext The application context used to access configuration changes.
 * @param logger The [Logger] instance used to record orientation change events.
 * @param additionalExtractors A list of [EventAttributesExtractor]s to extract and attach additional attributes.
 */
internal class ScreenOrientationDetector(
    applicationContext: Context,
    private val logger: Logger,
    private val additionalExtractors: List<EventAttributesExtractor<Orientation>> = emptyList(),
) : ComponentCallbacks {
    private var currentOrientation: Int = applicationContext.resources.configuration.orientation

    internal companion object {
        const val EVENT_NAME = "device.screen_orientation"
    }

    private fun emitLog(
        orientation: Orientation,
        body: String,
    ) {
        val attributesBuilder = Attributes.builder()
        additionalExtractors.forEach {
            it
                .extract(
                    io.opentelemetry.context.Context
                        .current(),
                    orientation,
                ).also { attributes ->
                    attributesBuilder.putAll(attributes)
                }
        }

        logger
            .logRecordBuilder()
            .setEventName(EVENT_NAME)
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
                "Screen orientation changed to ${config.orientation.name}.",
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        // no op
    }
}
