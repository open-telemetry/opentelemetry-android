/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.extensions.ApiExtension
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.Clock

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
     * Get the [SessionProvider] associated with this instance.
     */
    val sessionProvider: SessionProvider

    /**
     * Get the [Clock] used by this instance for time-related operations.
     */
    val clock: Clock

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
     * Returns the [ApiExtension] registered under [type], or null when this instance doesn't
     * provide it. Extensions are manual APIs contributed by optional modules, see [ApiExtension].
     *
     * Kotlin callers can use the reified [io.opentelemetry.android.extensions.extension] helper
     * instead.
     *
     * The default returns null so that existing implementations of this interface keep working.
     */
    fun <T : ApiExtension> getExtension(type: Class<T>): T? = null

    /**
     * Initiates orderly shutdown of this OpenTelemetryRum instance. After this method completes,
     * the instance should be considered invalid and no longer used.
     */
    fun shutdown()
}
