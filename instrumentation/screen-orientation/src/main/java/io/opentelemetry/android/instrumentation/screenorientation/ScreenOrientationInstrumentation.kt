/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.screenorientation

import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation

/**
 * An Android instrumentation module that installs and manages [ScreenOrientationDetector].
 */
@AutoService(AndroidInstrumentation::class)
class ScreenOrientationInstrumentation : AndroidInstrumentation {
    private var detector: ScreenOrientationDetector? = null

    override fun install(context: Context, openTelemetryRum: OpenTelemetryRum) {
        val logger =
            openTelemetryRum.openTelemetry
                .logsBridge
                .loggerBuilder("io.opentelemetry.$name")
                .build()
        val applicationContext = context.applicationContext
        detector = ScreenOrientationDetector(applicationContext, logger)
        applicationContext.registerComponentCallbacks(detector)
    }

    override fun uninstall(context: Context, openTelemetryRum: OpenTelemetryRum) {
        detector?.let {
            context.applicationContext.unregisterComponentCallbacks(it)
        }
    }

    override val name: String = "screen_orientation"
}
