/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import io.opentelemetry.android.config.OtelRumConfig

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
     * Returns a no-op implementation of [OpenTelemetryRum].
     */
    @JvmStatic
    fun noop(): OpenTelemetryRum = NoopOpenTelemetryRum
}
