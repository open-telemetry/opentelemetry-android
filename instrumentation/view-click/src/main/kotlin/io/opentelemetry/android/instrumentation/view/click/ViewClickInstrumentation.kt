/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.incubator.logs.ExtendedLogger

@AutoService(AndroidInstrumentation::class)
class ViewClickInstrumentation : AndroidInstrumentation {
    override val name: String = "view.click"

    override fun install(ctx: InstallationContext) {
        ctx.application.registerActivityLifecycleCallbacks(
            ViewClickActivityCallback(
                ViewClickEventGenerator(
                    ctx.openTelemetry
                        .logsBridge
                        .loggerBuilder("io.opentelemetry.android.view.click")
                        .build() as ExtendedLogger,
                ),
            ),
        )
    }
}
