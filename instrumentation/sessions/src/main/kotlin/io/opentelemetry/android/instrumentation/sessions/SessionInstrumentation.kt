/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.incubator.logs.ExtendedLogger

class SessionInstrumentation : AndroidInstrumentation {
    override fun install(ctx: InstallationContext) {
        val eventLogger: ExtendedLogger =
            ctx.openTelemetry.logsBridge
                .loggerBuilder("otel.session")
                .build() as ExtendedLogger
        ctx.sessionManager.addObserver(SessionIdEventSender(eventLogger))
    }
}
