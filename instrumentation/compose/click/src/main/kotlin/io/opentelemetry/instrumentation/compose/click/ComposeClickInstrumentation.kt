/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.click

import android.app.Application
import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.kotlin.toOtelKotlinApi

@AutoService(AndroidInstrumentation::class)
class ComposeClickInstrumentation : AndroidInstrumentation {
    override val name: String = "compose.click"

    private var activityCallback: ComposeClickActivityCallback? = null
    private var eventGenerator: ComposeClickEventGenerator? = null

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val generator =
            ComposeClickEventGenerator(
                openTelemetryRum.openTelemetry
                    .toOtelKotlinApi()
                    .loggerProvider
                    .getLogger("io.opentelemetry.android.instrumentation.compose.click"),
            )
        val callback = ComposeClickActivityCallback(generator)
        (context as? Application)?.registerActivityLifecycleCallbacks(callback)
        activityCallback = callback
        eventGenerator = generator
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        (context as? Application)?.let { app ->
            activityCallback?.let { callback ->
                app.unregisterActivityLifecycleCallbacks(callback)
            }
        }
        eventGenerator?.stopTracking()
        activityCallback = null
        eventGenerator = null
    }
}
