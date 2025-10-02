/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionPublisher

@AutoService(AndroidInstrumentation::class)
class SessionInstrumentation : AndroidInstrumentation {
    override val name: String = "session"

    override fun install(ctx: InstallationContext) {
        val eventLogger =
            ctx.openTelemetry.logsBridge
                .loggerBuilder("otel.session")
                .build()
        val sessionProvider = ctx.sessionProvider
        if (sessionProvider is SessionPublisher) {
            sessionProvider.addObserver(SessionIdEventSender(eventLogger))
        }
    }
}
