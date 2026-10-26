/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.OrderedAndroidInstrumentation
import io.opentelemetry.android.session.SessionPublisher
import io.opentelemetry.kotlin.toOtelKotlinApi

@AutoService(AndroidInstrumentation::class)
class SessionInstrumentation : OrderedAndroidInstrumentation {
    override val name: String = "session"

    override val installFirst: Boolean = true

    override val installBefore: Set<String> = setOf("native-crash")

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val eventLogger =
            openTelemetryRum.openTelemetry
                .toOtelKotlinApi()
                .loggerProvider
                .getLogger("otel.session")
        val sessionProvider = openTelemetryRum.sessionProvider
        if (sessionProvider is SessionPublisher) {
            sessionProvider.addObserver(SessionIdEventSender(eventLogger))
        }
    }
}
