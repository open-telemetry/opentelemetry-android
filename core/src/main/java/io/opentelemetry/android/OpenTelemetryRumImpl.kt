/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.sdk.OpenTelemetrySdk

internal class OpenTelemetryRumImpl(
    private val openTelemetrySdk: OpenTelemetrySdk,
    private val sessionProvider: SessionProvider,
    private val onShutdown: Runnable,
) : OpenTelemetryRum {
    private val logger: Logger =
        openTelemetrySdk.logsBridge
            .loggerBuilder("io.opentelemetry.rum.events")
            .build()

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

    override fun shutdown() {
        onShutdown.run()
    }
}
