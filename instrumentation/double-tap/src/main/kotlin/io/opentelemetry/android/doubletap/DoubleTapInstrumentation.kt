package io.opentelemetry.android.doubletap

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext

@AutoService(AndroidInstrumentation::class)
class DoubleTapInstrumentation: AndroidInstrumentation {
    override val name: String = "doubletap"

    override fun install(ctx: InstallationContext) {
        ctx.application?.registerActivityLifecycleCallbacks(
            DoubleTapActivityCallback(
                DoubleTapEventGenerator(
                    ctx.openTelemetry
                        .logsBridge
                        .loggerBuilder("io.opentelemetry.android.instrumentation.doubletap")
                        .build(),
                ),
            ),
        )
    }

}
