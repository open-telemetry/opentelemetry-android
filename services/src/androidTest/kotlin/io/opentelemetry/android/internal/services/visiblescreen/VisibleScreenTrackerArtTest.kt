/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen

import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisibleScreenTrackerArtTest {
    @Test
    fun pausedFragmentIsNoLongerVisible() {
        val tracker = VisibleScreenTrackerImpl(ApplicationProvider.getApplicationContext())
        val fragment = Fragment()

        tracker.fragmentResumed(fragment)
        tracker.fragmentPaused(fragment)

        assertEquals("unknown", tracker.currentlyVisibleScreen)
    }
}
