/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.Clock

internal class OpenTelemetryRumImpl(
    private val openTelemetrySdk: OpenTelemetrySdk,
    private val sessionProvider: SessionProvider,
    private val context: Context,
    private val clock: Clock,
    private val onShutdown: Runnable,
) : OpenTelemetryRum {
    private val logger: Logger =
        openTelemetrySdk.logsBridge
            .loggerBuilder("io.opentelemetry.rum.events")
            .build()

    private val manuallyInstalledInstrumentations = mutableListOf<AndroidInstrumentation>()

    override val openTelemetry: OpenTelemetry = openTelemetrySdk

    override fun getRumSessionId(): String = sessionProvider.getSessionId()

    override fun emitEvent(
        eventName: String,
        body: String,
        attributes: Attributes,
    ) {
        val logRecordBuilder = logger.logRecordBuilder()
        logRecordBuilder
            .setEventName(eventName)
            .setBody(body)
            .setAllAttributes(attributes)
            .emit()
    }

    override fun installInstrumentation(instrumentation: AndroidInstrumentation) {
        val ctx = InstallationContext(context, openTelemetrySdk, sessionProvider, clock)
        instrumentation.install(ctx)
        manuallyInstalledInstrumentations.add(instrumentation)
    }

    override fun shutdown() {
        val ctx = InstallationContext(context, openTelemetrySdk, sessionProvider, clock)
        for (instrumentation in manuallyInstalledInstrumentations) {
            instrumentation.uninstall(ctx)
        }
        onShutdown.run()
    }
}
