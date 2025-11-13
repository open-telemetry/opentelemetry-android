/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.OpenTelemetrySdk

object RumBuilder {
    /**
     * Returns a new [io.opentelemetry.android.OpenTelemetryRumBuilder] for [OpenTelemetryRum] with a default
     * configuration. Use this version if you would like to configure individual aspects of the
     * OpenTelemetry SDK but would still prefer to allow OpenTelemetry RUM to create the SDK for
     * you. For additional configuration, call the two-argument version of build and pass it your
     * [io.opentelemetry.android.config.OtelRumConfig] instance. If you would like to "bring your own" SDK, call the
     * two-argument version that takes the SDK as a parameter.
     *
     * @param context The [android.content.Context] that is being instrumented.
     */
    @JvmStatic
    @JvmOverloads
    fun builder(
        context: Context,
        config: OtelRumConfig = OtelRumConfig(),
    ): OpenTelemetryRumBuilder = OpenTelemetryRumBuilder.create(context, config)

    /**
     * Returns a new [io.opentelemetry.android.SdkPreconfiguredRumBuilder] for [OpenTelemetryRum]. This version
     * requires the user to preconfigure and create their own OpenTelemetrySdk instance. If you
     * prefer to use the builder to configure individual aspects of the OpenTelemetry SDK and to
     * create and manage it for you, call the one-argument version.
     *
     * Specific consideration should be given to the creation of your provided SDK to ensure that
     * the [SdkTracerProvider], [SdkMeterProvider], and [SdkLoggerProvider] are
     * configured correctly for your target RUM provider.
     *
     * @param context The [Context] that is being instrumented.
     * @param openTelemetrySdk The [io.opentelemetry.sdk.OpenTelemetrySdk] that the user has already created.
     * @param config The [io.opentelemetry.android.config.OtelRumConfig] instance.
     * @param sessionProvider The [io.opentelemetry.android.session.SessionProvider] instance.
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
