/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.fragments

import androidx.fragment.app.Fragment
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VisibleFragmentTrackerTest {
    private lateinit var service: VisibleScreenTracker
    private lateinit var fragment: Fragment
    private lateinit var visibleFragmentTracker: VisibleFragmentTracker

    @BeforeEach
    fun setUp() {
        service = mockk()
        fragment = mockk()
        visibleFragmentTracker = VisibleFragmentTracker(service)
    }

    @Test
    fun `Track fragment resumed`() {
        every { service.fragmentResumed(any()) } just Runs

        visibleFragmentTracker.onFragmentResumed(mockk(), fragment)

        verify {
            service.fragmentResumed(fragment)
        }
    }

    @Test
    fun `Track fragment paused`() {
        every { service.fragmentPaused(any()) } just Runs

        visibleFragmentTracker.onFragmentPaused(mockk(), fragment)

        verify {
            service.fragmentPaused(fragment)
        }
    }
}
