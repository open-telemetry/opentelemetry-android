/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.ktx.setSessionIdentifiersWith
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

    /**
     * Emits a RUM event with automatic session identifier tracking.
     *
     * Session identifiers (both current and previous, if applicable) are automatically attached to
     * the event, enabling correlation of events within and across session boundaries.
     *
     * @param eventName the name of the event
     * @param body the body content of the event
     * @param attributes additional attributes to attach to the event
     */
    override fun emitEvent(
        eventName: String,
        body: String,
        attributes: Attributes,
    ) {
        logger
            .logRecordBuilder()
            .setSessionIdentifiersWith(sessionProvider)
            .setEventName(eventName)
            .setBody(body)
            .setAllAttributes(attributes)
            .emit()
    }

    override fun shutdown() {
        onShutdown.run()
    }
}
