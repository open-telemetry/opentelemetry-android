/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reports resolved screen names to the [VisibleScreenTracker] on behalf of a single attached
 * controller, identifying itself as the owner of each report.
 *
 * Each attached controller owns its own reporter. A nested or sibling controller that has since
 * recorded a newer destination therefore keeps it when an older controller leaves the composition,
 * because the tracker only honours a clear from the owner of the destination it still holds, even
 * when both controllers reported the same name.
 */
internal class NavigationDestinationReporter(
    private val visibleScreenTracker: VisibleScreenTracker,
) {
    private val hasReported = AtomicBoolean(false)

    fun report(destinationName: String) {
        hasReported.set(true)
        visibleScreenTracker.navigationDestinationChanged(this, destinationName)
    }

    fun clear() {
        if (hasReported.getAndSet(false)) {
            visibleScreenTracker.navigationDestinationCleared(this)
        }
    }
}
