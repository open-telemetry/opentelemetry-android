/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.kotlin.getLogger
import io.opentelemetry.kotlin.toOtelKotlinApi

@AutoService(AndroidInstrumentation::class)
class ViewClickInstrumentation : AndroidInstrumentation {
    override val name: String = "view.click"

    override fun install(ctx: InstallationContext) {
        val otel = ctx.openTelemetry.toOtelKotlinApi()
        ctx.application?.registerActivityLifecycleCallbacks(
            ViewClickActivityCallback(
                ViewClickEventGenerator(
                    otel.getLogger("io.opentelemetry.android.instrumentation.view.click")
                ),
            ),
        )
    }
}
