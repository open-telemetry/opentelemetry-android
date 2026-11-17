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
import io.opentelemetry.kotlin.toOtelKotlinApi

@AutoService(AndroidInstrumentation::class)
class SessionInstrumentation : AndroidInstrumentation {
    private var sessionPublisher: SessionPublisher? = null
    private var sessionObserver: SessionIdEventSender? = null

    override val name: String = "session"

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        removeObserver()
        val eventLogger =
            openTelemetryRum.openTelemetry
                .toOtelKotlinApi()
                .loggerProvider
                .getLogger("otel.session")
        val sessionProvider = openTelemetryRum.sessionProvider
        if (sessionProvider is SessionPublisher) {
            val observer = SessionIdEventSender(eventLogger)
            sessionProvider.addObserver(observer)
            sessionPublisher = sessionProvider
            sessionObserver = observer
        }
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        removeObserver()
    }

    private fun removeObserver() {
        val publisher = sessionPublisher
        val observer = sessionObserver
        sessionPublisher = null
        sessionObserver = null
        if (publisher != null && observer != null) {
            publisher.removeObserver(observer)
        }
    }
}
