/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider

class SessionInstrumentation : AndroidInstrumentation {
    override fun install(ctx: InstallationContext) {
        val eventLogger =
            SdkEventLoggerProvider
                .create(ctx.openTelemetry.logsBridge)
                .get(OpenTelemetryRum::class.java.simpleName)

        ctx.sessionManager.addObserver(SessionIdEventSender(eventLogger))
    }
}
