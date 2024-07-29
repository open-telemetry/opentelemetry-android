/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import android.app.Application
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.internal.initialization.InitializationEvents

@AutoService(AndroidInstrumentation::class)
class StartupInstrumentation : AndroidInstrumentation {
    override fun install(
        application: Application,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val events = InitializationEvents.get()
        if (events is SdkInitializationEvents) {
            events.finish(openTelemetryRum.openTelemetry)
        }
    }
}
