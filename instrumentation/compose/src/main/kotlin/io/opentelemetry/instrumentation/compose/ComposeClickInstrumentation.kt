/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.incubator.logs.ExtendedLogger

@AutoService(AndroidInstrumentation::class)
class ComposeClickInstrumentation : AndroidInstrumentation {
    override val name: String = "compose"

    override fun install(ctx: InstallationContext) {
        ctx.application.registerActivityLifecycleCallbacks(
            ComposeClickActivityCallback(
                ComposeClickEventGenerator(
                    ctx.openTelemetry
                        .logsBridge
                        .loggerBuilder("io.opentelemetry.android.instrumentation.compose")
                        .build() as ExtendedLogger,
                ),
            ),
        )
    }
}
