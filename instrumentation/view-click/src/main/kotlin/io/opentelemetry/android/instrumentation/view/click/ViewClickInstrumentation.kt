/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.app.Application
import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.kotlin.toOtelKotlinApi

@AutoService(AndroidInstrumentation::class)
class ViewClickInstrumentation : AndroidInstrumentation {
    override val name: String = "view.click"

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val scopeName = "io.opentelemetry.android.instrumentation.view.click"
        (context as? Application)?.registerActivityLifecycleCallbacks(
            ViewClickActivityCallback(
                ViewClickEventGenerator(
                    eventLogger = openTelemetryRum.openTelemetry.logsBridge.get(scopeName),
                    clickEventLogger =
                        openTelemetryRum.openTelemetry
                            .toOtelKotlinApi()
                            .loggerProvider
                            .getLogger(scopeName),
                ),
            ),
        )
    }
}
