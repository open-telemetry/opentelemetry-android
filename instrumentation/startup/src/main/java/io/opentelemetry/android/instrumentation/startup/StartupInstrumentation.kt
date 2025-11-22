/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.internal.initialization.InitializationEvents

@AutoService(AndroidInstrumentation::class)
class StartupInstrumentation : AndroidInstrumentation {
    override val name: String = "startup"

    override fun install(ctx: InstallationContext) {
        val events = InitializationEvents.get()
        if (events is SdkInitializationEvents) {
            events.finish(ctx.openTelemetry, ctx.sessionProvider)
        }
    }
}
