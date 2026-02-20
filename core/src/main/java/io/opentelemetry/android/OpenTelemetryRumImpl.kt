/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstrumentationParams
import io.opentelemetry.android.instrumentation.InstrumentationParamsImpl
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.Clock

internal class OpenTelemetryRumImpl(
    private val instrumentationParams: InstrumentationParams
) : OpenTelemetryRum {
    private val logger: Logger =
        instrumentationParams.openTelemetry.logsBridge
            .loggerBuilder("io.opentelemetry.rum.events")
            .build()

    override val openTelemetry: OpenTelemetry = instrumentationParams.openTelemetry

    override fun getRumSessionId(): String = instrumentationParams.sessionProvider.getSessionId()

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

    override fun install(instrumentation: AndroidInstrumentation) {
        instrumentation.install(instrumentationParams)
    }
}
