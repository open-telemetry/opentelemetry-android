/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationDestinationReporterTest {
    private val visibleScreenTracker = mockk<VisibleScreenTracker>(relaxed = true)
    private val reporter = NavigationDestinationReporter(visibleScreenTracker)

    @Test
    fun `clears using the last name it reported`() {
        reporter.report("a")
        reporter.report("b")
        reporter.clear()

        verify(exactly = 1) { visibleScreenTracker.navigationDestinationCleared("b") }
        verify(exactly = 0) { visibleScreenTracker.navigationDestinationCleared("a") }
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

        verify(exactly = 1) { visibleScreenTracker.navigationDestinationCleared("a") }
    }
}
