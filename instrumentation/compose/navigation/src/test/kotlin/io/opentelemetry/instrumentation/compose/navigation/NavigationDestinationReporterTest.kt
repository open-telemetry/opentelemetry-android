/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import org.junit.Test

class NavigationDestinationReporterTest {
    private val visibleScreenTracker = mockk<VisibleScreenTracker>(relaxed = true)
    private val reporter = NavigationDestinationReporter(visibleScreenTracker)

    @Test
    fun `reports and clears as its own owner`() {
        reporter.report("a")
        reporter.report("b")
        reporter.clear()

        verify(exactly = 1) { visibleScreenTracker.navigationDestinationChanged(reporter, "a") }
        verify(exactly = 1) { visibleScreenTracker.navigationDestinationChanged(reporter, "b") }
        verify(exactly = 1) { visibleScreenTracker.navigationDestinationCleared(reporter) }
    }

    @Test
    fun `clearing without having reported anything does not touch the tracker`() {
        reporter.clear()

        verify(exactly = 0) { visibleScreenTracker.navigationDestinationCleared(any()) }
    }

    @Test
    fun `clearing twice only clears once`() {
        reporter.report("a")
        reporter.clear()
        reporter.clear()

        verify(exactly = 1) { visibleScreenTracker.navigationDestinationCleared(reporter) }
    }
}
