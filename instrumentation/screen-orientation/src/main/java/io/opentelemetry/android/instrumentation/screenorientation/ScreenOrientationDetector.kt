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
import io.opentelemetry.api.logs.Logger

/**
 * Detects and logs screen orientation changes in the Android application.
 *
 * This detector registers as a [ComponentCallbacks] to listen for configuration changes
 * related to screen orientation (portrait, landscape). When a change occurs, it emits
 * a log event via the provided [Logger].
 *
 * @param applicationContext The application context used to access configuration changes.
 * @param logger The [Logger] instance used to record orientation change events.
 */
internal class ScreenOrientationDetector(
    applicationContext: Context,
    private val logger: Logger,
) : ComponentCallbacks {
    private var currentOrientation: Int = applicationContext.resources.configuration.orientation

    internal companion object {
        const val EVENT_NAME = "device.screen_orientation"
        const val SCREEN_ORIENTATION = "screen.orientation"
    }

    private fun emitLog(orientation: String) {
        logger
            .logRecordBuilder()
            .setEventName(EVENT_NAME)
            .setAttribute(SCREEN_ORIENTATION, orientation)
            .emit()
    }

    private val Int.name: String
        get() {
            return when (this) {
                ORIENTATION_LANDSCAPE -> "landscape"
                ORIENTATION_PORTRAIT -> "portrait"
                else -> "undefined"
            }
        }

    override fun onConfigurationChanged(config: Configuration) {
        if (config.orientation != currentOrientation) {
            currentOrientation = config.orientation
            emitLog(config.orientation.name)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        // no op
    }
}
