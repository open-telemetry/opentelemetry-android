/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.internal.initialization.InitializationEvents

@AutoService(AndroidInstrumentation::class)
class StartupInstrumentation : AndroidInstrumentation {
    override val name: String = "startup"

    override fun install(context: Context, openTelemetryRum: OpenTelemetryRum) {
        val events = InitializationEvents.get()
        if (events is SdkInitializationEvents) {
            events.finish(openTelemetryRum.openTelemetry)
        }
    }
}
