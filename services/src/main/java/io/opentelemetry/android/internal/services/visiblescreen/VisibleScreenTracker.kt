/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen

import android.app.Activity
import androidx.fragment.app.Fragment
import io.opentelemetry.android.internal.services.Service

/**
 * Wherein we do our best to figure out what "screen" is visible and what was the previously visible
 * "screen".
 *
 * In general, we favor using the last fragment that was resumed, but fall back to the last
 * resumed activity in case we don't have a fragment.
 *
 * We always ignore NavHostFragment instances since they aren't ever visible to the user.
 *
 * We have to treat DialogFragments slightly differently since they don't replace the launching
 * screen, and the launching screen never leaves visibility.
 */
interface VisibleScreenTracker : Service {
    val previouslyVisibleScreen: String?
    val currentlyVisibleScreen: String

    fun activityResumed(activity: Activity)

    fun activityPaused(activity: Activity)

    fun fragmentResumed(fragment: Fragment)

    fun fragmentPaused(fragment: Fragment)
}
