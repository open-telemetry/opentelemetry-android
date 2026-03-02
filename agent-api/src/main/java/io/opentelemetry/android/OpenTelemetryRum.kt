/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes

/**
 * Entrypoint for the OpenTelemetry Real User Monitoring library for Android.
 */
interface OpenTelemetryRum {
    /**
     * Get a handle to the instance of the OpenTelemetry API that this
     * instance is using for instrumentation.
     */
    val openTelemetry: OpenTelemetry

    /**
     * Get the client session ID associated with this instance of the RUM instrumentation library.
     * Note: this value will change throughout the lifetime of an application instance, so it is
     * recommended that you do not cache this value, but always retrieve it from here when needed.
     */
    fun getRumSessionId(): String

    /**
     * Emits an event with the specified name, body, and attributes.
     *
     * Implementations of this method should define how the event is handled and recorded.
     *
     * @param eventName The name of the event to emit.
     * @param body The body of the event, typically containing additional data.
     * @param attributes The attributes associated with the event, providing metadata.
     */
    fun emitEvent(
        eventName: String,
        body: String = "",
        attributes: Attributes = Attributes.empty(),
    )

    /**
     * Installs an [AndroidInstrumentation] using the internal context of this [OpenTelemetryRum]
     * instance. This creates the appropriate [io.opentelemetry.android.instrumentation.InstallationContext]
     * with the correct context objects that were used to build this instance.
     *
     * @param instrumentation The instrumentation to install.
     */
    fun installInstrumentation(instrumentation: AndroidInstrumentation)

    /**
     * Initiates orderly shutdown of this OpenTelemetryRum instance. After this method completes,
     * the instance should be considered invalid and no longer used.
     */
    fun shutdown()
}
