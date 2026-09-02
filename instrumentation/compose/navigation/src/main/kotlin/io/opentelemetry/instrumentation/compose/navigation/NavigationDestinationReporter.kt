/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import java.util.concurrent.atomic.AtomicReference

/**
 * Reports resolved screen names to the [VisibleScreenTracker] on behalf of a single attached
 * controller, remembering the last name it reported so that [clear] can name it.
 *
 * Each attached controller owns its own reporter. A nested or sibling controller that has since
 * recorded a newer destination therefore keeps it when an older controller leaves the composition,
 * because the tracker only honours a clear for the destination it still holds.
 */
internal class NavigationDestinationReporter(
    private val visibleScreenTracker: VisibleScreenTracker,
) {
    private val lastReportedName = AtomicReference<String?>()

    fun report(destinationName: String) {
        lastReportedName.set(destinationName)
        visibleScreenTracker.navigationDestinationChanged(destinationName)
    }

    fun clear() {
        lastReportedName.getAndSet(null)?.let(visibleScreenTracker::navigationDestinationCleared)
    }
}
