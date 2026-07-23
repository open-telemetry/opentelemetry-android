/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.logs.Logger

internal const val INSTRUMENTATION_SCOPE_NAME =
    "io.opentelemetry.android.instrumentation.compose.navigation"

/**
 * Receives the resolved screen name each time a navigation completes (the destination changes).
 * Telemetry emission is tracked in issue #1909, pending the mobile semantic-conventions
 * discussion on modelling navigation.
 */
internal class NavigationEmitter(
    // Kept for the event emission implementation tracked in issue #1909.
    @Suppress("UnusedPrivateProperty", "unused")
    private val eventLogger: Logger,
) {
    constructor(rum: OpenTelemetryRum) : this(
        rum.openTelemetry.logsBridge
            .loggerBuilder(INSTRUMENTATION_SCOPE_NAME)
            .build(),
    )

    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun onScreenView(screenName: String) {
        // TODO: https://github.com/open-telemetry/opentelemetry-android/issues/1909
    }
}
