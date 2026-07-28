/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.compose.rememberNavController
import io.opentelemetry.android.OpenTelemetryRum

/**
 * Attaches OpenTelemetry navigation instrumentation to this [NavController]: on every completed
 * destination change the new destination is resolved to a screen name and handed to the
 * instrumentation backed by [rum] (telemetry emission is tracked in issue #1909). The listener is
 * scoped to the composition via [DisposableEffect] and removed when the controller leaves it.
 *
 * Works for any controller you already hold, including nested/child controllers, not just the host
 * controller returned by `rememberNavController()`. The receiver is returned with its static type
 * preserved (e.g. [NavHostController]), so the call can be grafted onto a `rememberNavController()`
 * result (the auto-instrumentation seam) without down-typing.
 *
 * @param rum the [OpenTelemetryRum] instance to emit through.
 * @param screenName maps a destination (and its arguments) to a screen name. Defaults to the route
 *   pattern to avoid leaking PII.
 */
@Composable
fun <T : NavController> T.withOpenTelemetry(
    rum: OpenTelemetryRum,
    screenName: (NavDestination, Bundle?) -> String = ::defaultScreenName,
): T {
    // The listener is registered once per controller (the effect is keyed only on `this`), yet it
    // must always use the latest `rum`/`screenName`; rememberUpdatedState lets a changed value take
    // effect without re-registering.
    val emitter = remember(rum) { NavigationEmitter(rum) }
    val currentEmitter by rememberUpdatedState(emitter)
    val currentScreenName by rememberUpdatedState(screenName)
    DisposableEffect(this) {
        val listener = attachOpenTelemetry({ currentEmitter }, { currentScreenName })
        onDispose { removeOnDestinationChangedListener(listener) }
    }
    return this
}

/**
 * Drop-in replacement for `rememberNavController()` that attaches OpenTelemetry navigation
 * instrumentation; it delegates to [withOpenTelemetry]. Accepts the same [screenName] override so
 * callers don't have to switch to chaining [withOpenTelemetry].
 */
@Composable
fun rememberObservedNavController(
    rum: OpenTelemetryRum,
    screenName: (NavDestination, Bundle?) -> String = ::defaultScreenName,
    vararg navigators: Navigator<out NavDestination>,
): NavHostController = rememberNavController(*navigators).withOpenTelemetry(rum, screenName)

/**
 * Registers an [NavController.OnDestinationChangedListener] that resolves a screen name and hands
 * it to the [emitter] on each destination change. The [emitter] and [screenName] are resolved
 * lazily on every change, so callers can back them with values that vary over time.
 *
 * This is the non-Compose core of [withOpenTelemetry], extracted so it can be tested by invoking
 * the returned listener directly.
 */
internal fun NavController.attachOpenTelemetry(
    emitter: () -> NavigationEmitter,
    screenName: () -> (NavDestination, Bundle?) -> String,
): NavController.OnDestinationChangedListener {
    val listener =
        NavController.OnDestinationChangedListener { _, destination, arguments ->
            emitter().onScreenView(screenName()(destination, arguments))
        }
    addOnDestinationChangedListener(listener)
    return listener
}
