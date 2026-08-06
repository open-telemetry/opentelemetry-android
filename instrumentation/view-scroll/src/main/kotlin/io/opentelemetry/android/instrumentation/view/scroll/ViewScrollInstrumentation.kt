/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.scroll

import android.app.Application
import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.view.common.ViewClickActivityCallback

@AutoService(AndroidInstrumentation::class)
class ViewScrollInstrumentation : AndroidInstrumentation {
    override val name: String = "view.scroll"

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        (context as? Application)?.registerActivityLifecycleCallbacks(
            ViewClickActivityCallback(
                ViewScrollEventGenerator(
                    openTelemetryRum.openTelemetry
                        .logsBridge
                        .loggerBuilder("io.opentelemetry.android.instrumentation.view.scroll")
                        .build(),
                ),
            ),
        )
    }
}
