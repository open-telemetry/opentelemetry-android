/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.common.internal.SemconvCompat.Companion.map
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi

internal const val INSTRUMENTATION_SCOPE_NAME =
    "io.opentelemetry.android.instrumentation.compose.navigation"

// Provisional event name pending the mobile semconv SIG agreement.
internal const val SCREEN_VIEW_EVENT_NAME = "app.screen.view"

/**
 * Emits a screen-view event each time the navigation destination changes.
 *
 * The screen name is also stamped onto the event via the [APP_SCREEN_NAME] semantic attribute.
 * Feeding the route into OpenTelemetry Android's central screen-attribution mechanism (so that
 * every downstream span/log is stamped) is intentionally left to a later phase.
 */
internal class NavigationEmitter(
    private val eventLogger: Logger,
) {
    constructor(rum: OpenTelemetryRum) : this(
        rum.openTelemetry.logsBridge
            .loggerBuilder(INSTRUMENTATION_SCOPE_NAME)
            .build(),
    )

    @OptIn(IncubatingApi::class)
    fun onScreenView(screenName: String) {
        eventLogger
            .logRecordBuilder()
            .setEventName(SCREEN_VIEW_EVENT_NAME)
            .setAttribute(stringKey(map(APP_SCREEN_NAME)), screenName)
            .emit()
    }
}
