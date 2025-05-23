/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionPublisher
import io.opentelemetry.api.incubator.logs.ExtendedLogger

class SessionInstrumentation : AndroidInstrumentation {
    override val name: String = "session"

    override fun install(ctx: InstallationContext) {
        val eventLogger: ExtendedLogger =
            ctx.openTelemetry.logsBridge
                .loggerBuilder("otel.session")
                .build() as ExtendedLogger
        val sessionManager = ctx.sessionManager
        if (sessionManager is SessionPublisher) {
            sessionManager.addObserver(SessionIdEventSender(eventLogger))
        }
    }
}
