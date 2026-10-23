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

    private var activityCallback: ViewClickActivityCallback? = null
    private var eventGenerator: ViewClickEventGenerator? = null

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val application = context as? Application ?: return
        val scopeName = "io.opentelemetry.android.instrumentation.view.click"
        val generator =
            ViewClickEventGenerator(
                eventLogger = openTelemetryRum.openTelemetry.logsBridge.get(scopeName),
                clickEventLogger =
                    openTelemetryRum.openTelemetry
                        .toOtelKotlinApi()
                        .loggerProvider
                        .getLogger(scopeName),
            )
        val callback = ViewClickActivityCallback(generator)
        application.registerActivityLifecycleCallbacks(callback)
        activityCallback = callback
        eventGenerator = generator
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        (context as? Application)?.let { app ->
            activityCallback?.let { callback ->
                callback.isEnabled = false
                app.unregisterActivityLifecycleCallbacks(callback)
            }
        }
        eventGenerator?.stopTracking()
        activityCallback = null
        eventGenerator = null
    }
}
