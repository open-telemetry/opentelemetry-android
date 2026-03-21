/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.session.SessionPublisher

@AutoService(AndroidInstrumentation::class)
class SessionInstrumentation : AndroidInstrumentation {
    override val name: String = "session"

    override fun install(context: Context, openTelemetryRum: OpenTelemetryRum) {
        val eventLogger =
            openTelemetryRum.openTelemetry.logsBridge
                .loggerBuilder("otel.session")
                .build()
        val sessionProvider = openTelemetryRum.sessionProvider
        if (sessionProvider is SessionPublisher) {
            sessionProvider.addObserver(SessionIdEventSender(eventLogger))
        }
    }
}
