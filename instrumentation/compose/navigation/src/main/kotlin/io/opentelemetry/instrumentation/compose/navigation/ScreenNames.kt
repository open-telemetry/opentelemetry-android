/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import android.os.Bundle
import androidx.navigation.NavDestination

/**
 * Default screen-name resolver.
 *
 * Uses the route *pattern* (for example `user/{id}`, not the filled-in arguments) to avoid leaking
 * PII. Callers can supply their own resolver (including one that reads [arguments]) via
 * [withOpenTelemetry].
 */
internal fun defaultScreenName(
    destination: NavDestination,
    @Suppress("UNUSED_PARAMETER") arguments: Bundle?,
): String = destination.route ?: destination.label?.toString() ?: destination.navigatorName
