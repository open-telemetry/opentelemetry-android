/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.screenorientation

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext

/**
 * An Android instrumentation module that installs and manages [ScreenOrientationDetector].
 */
@AutoService(AndroidInstrumentation::class)
class ScreenOrientationInstrumentation : AndroidInstrumentation {
    private lateinit var detector: ScreenOrientationDetector

    override fun install(ctx: InstallationContext) {
        val logger =
            ctx.openTelemetry
                .logsBridge
                .loggerBuilder("io.opentelemetry.$name")
                .build()
        val applicationContext = ctx.context.applicationContext
        detector = ScreenOrientationDetector(applicationContext, logger)
        applicationContext.registerComponentCallbacks(detector)
    }

    override fun uninstall(ctx: InstallationContext) {
        if (!::detector.isInitialized) return

        ctx.context.applicationContext.unregisterComponentCallbacks(detector)
    }

    override val name: String = "screen_orientation"
}
