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

@AutoService(AndroidInstrumentation::class)
class ViewClickInstrumentation : AndroidInstrumentation {
    override val name: String = "view.click"

    override fun install(context: Context, openTelemetryRum: OpenTelemetryRum) {
        (context as? Application)?.registerActivityLifecycleCallbacks(
            ViewClickActivityCallback(
                ViewClickEventGenerator(
                    openTelemetryRum.openTelemetry
                        .logsBridge
                        .loggerBuilder("io.opentelemetry.android.instrumentation.view.click")
                        .build(),
                ),
            ),
        )
    }
}
