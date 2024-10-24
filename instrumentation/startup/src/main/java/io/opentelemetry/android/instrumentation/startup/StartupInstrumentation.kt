/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import android.app.Application
import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.api.OpenTelemetry

@AutoService(AndroidInstrumentation::class)
class StartupInstrumentation : AndroidInstrumentation {
    override fun install(
        application: Application,
        openTelemetry: OpenTelemetry,
    ) {
        val events = InitializationEvents.get()
        if (events is SdkInitializationEvents) {
            events.finish(openTelemetry)
        }
    }
}
