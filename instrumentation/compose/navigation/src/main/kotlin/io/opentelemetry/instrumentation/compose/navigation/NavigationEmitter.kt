/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.semconv.events.AppNavigationCompleteEvent
import io.opentelemetry.api.logs.Logger

internal const val INSTRUMENTATION_SCOPE_NAME =
    "io.opentelemetry.android.instrumentation.compose.navigation"

/**
 * Emits an `app.navigation.complete` event for each screen name it is handed.
 *
 * Callers drive this from a [androidx.navigation.NavController.OnDestinationChangedListener], which
 * is not one-to-one with user-initiated navigation: it also replays the current destination when the
 * listener is registered, so re-attaching emits without a navigation having happened.
 */
internal class NavigationEmitter(
    private val eventLogger: Logger,
) {
    constructor(rum: OpenTelemetryRum) : this(
        rum.openTelemetry.logsBridge
            .loggerBuilder(INSTRUMENTATION_SCOPE_NAME)
            .build(),
    )

    fun onNavigation(screenName: String) {
        AppNavigationCompleteEvent(appNavigationDestinationName = screenName).emit(eventLogger)
    }
}
