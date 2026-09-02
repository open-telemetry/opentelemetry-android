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
 * In general, we favor the most recently reported navigation destination, then the last fragment
 * that was resumed, and fall back to the last resumed activity in case we have neither. While a
 * destination is recorded it outranks both, including a DialogFragment shown over it.
 *
 * Navigation destinations contribute only to the currently visible screen. The previously visible
 * screen is still derived from fragments and activities alone.
 *
 * We always ignore NavHostFragment instances since they aren't ever visible to the user. That
 * concerns the host fragment itself, not the destinations a navigation library reports to us.
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

    /**
     * Records the screen name of a navigation destination reached by a source that is neither an
     * Activity nor a Fragment, such as a Compose Navigation NavController. The recorded name takes
     * precedence over the last resumed fragment and activity until it is cleared.
     */
    fun navigationDestinationChanged(destinationName: String)

    /**
     * Clears the recorded navigation destination, so that the visible screen falls back to the last
     * resumed fragment, then the last resumed activity.
     *
     * The clear only applies if [destinationName] is still the recorded destination. A source that
     * has since been superseded by another one therefore cannot discard the newer destination.
     */
    fun navigationDestinationCleared(destinationName: String)
}
