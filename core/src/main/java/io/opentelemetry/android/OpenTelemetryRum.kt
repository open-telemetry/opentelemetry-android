/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk

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
     * Initiates orderly shutdown of this OpenTelemetryRum instance. After this method completes,
     * the instance should be considered invalid and no longer used.
     */
    fun shutdown()

    companion object {
        /**
         * Returns a new [OpenTelemetryRumBuilder] for [OpenTelemetryRum] with a default
         * configuration. Use this version if you would like to configure individual aspects of the
         * OpenTelemetry SDK but would still prefer to allow OpenTelemetry RUM to create the SDK for
         * you. For additional configuration, call the two-argument version of build and pass it your
         * [OtelRumConfig] instance. If you would like to "bring your own" SDK, call the
         * two-argument version that takes the SDK as a parameter.
         *
         * @param context The [Context] that is being instrumented.
         */
        @JvmStatic
        @JvmOverloads
        fun builder(
            context: Context,
            config: OtelRumConfig = OtelRumConfig(),
        ): OpenTelemetryRumBuilder = OpenTelemetryRumBuilder.create(context, config)

        /**
         * Returns a new [SdkPreconfiguredRumBuilder] for [OpenTelemetryRum]. This version
         * requires the user to preconfigure and create their own OpenTelemetrySdk instance. If you
         * prefer to use the builder to configure individual aspects of the OpenTelemetry SDK and to
         * create and manage it for you, call the one-argument version.
         *
         * Specific consideration should be given to the creation of your provided SDK to ensure that
         * the [SdkTracerProvider], [SdkMeterProvider], and [SdkLoggerProvider] are
         * configured correctly for your target RUM provider.
         *
         * @param context The [Context] that is being instrumented.
         * @param openTelemetrySdk The [OpenTelemetrySdk] that the user has already created.
         * @param config The [OtelRumConfig] instance.
         * @param sessionProvider The [SessionProvider] instance.
         */
        @JvmStatic
        fun builder(
            context: Context,
            openTelemetrySdk: OpenTelemetrySdk,
            config: OtelRumConfig,
            sessionProvider: SessionProvider,
        ): SdkPreconfiguredRumBuilder =
            SdkPreconfiguredRumBuilder(
                context,
                openTelemetrySdk,
                sessionProvider,
                config,
            )

        /**
         * Returns a no-op implementation of [OpenTelemetryRum].
         */
        @JvmStatic
        fun noop(): OpenTelemetryRum = NoopOpenTelemetryRum
    }
}
