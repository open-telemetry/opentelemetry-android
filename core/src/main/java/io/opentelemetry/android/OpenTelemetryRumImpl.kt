/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.extensions.ApiExtension
import io.opentelemetry.android.extensions.ApiExtensionRegistry
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.sdk.common.Clock

internal class OpenTelemetryRumImpl(
    override val openTelemetry: OpenTelemetry,
    override val sessionProvider: SessionProvider,
    override val clock: Clock,
    private val apiExtensions: ApiExtensionRegistry,
) : OpenTelemetryRum {
    var onShutdown: Runnable = Runnable {}
    private val logger: Logger =
        openTelemetry.logsBridge
            .loggerBuilder("io.opentelemetry.rum.events")
            .build()

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

    override fun <T : ApiExtension> getExtension(type: Class<T>): T? = apiExtensions.get(type)

    override fun shutdown() {
        onShutdown.run()
    }
}
