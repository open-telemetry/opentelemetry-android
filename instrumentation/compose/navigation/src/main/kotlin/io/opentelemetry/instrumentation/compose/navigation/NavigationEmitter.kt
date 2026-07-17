/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.semconv.events.AppScreenViewEvent
import io.opentelemetry.api.logs.Logger

internal const val INSTRUMENTATION_SCOPE_NAME =
    "io.opentelemetry.android.instrumentation.compose.navigation"

/**
 * Emits a screen-view event each time the navigation destination changes.
 */
internal class NavigationEmitter(
    private val eventLogger: Logger,
) {
    constructor(rum: OpenTelemetryRum) : this(
        rum.openTelemetry.logsBridge
            .loggerBuilder(INSTRUMENTATION_SCOPE_NAME)
            .build(),
    )

    fun onScreenView(screenName: String) {
        AppScreenViewEvent(appScreenName = screenName).emit(eventLogger)
    }
}
