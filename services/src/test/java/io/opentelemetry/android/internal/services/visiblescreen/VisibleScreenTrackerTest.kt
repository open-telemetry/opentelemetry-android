/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class VisibleScreenTrackerTest {
    @RelaxedMockK
    private lateinit var application: Application

    @Test
    fun verifyInitializationAndClose() {
        val visibleScreenTracker = this.visibleScreenService
        val callbacksCaptors: MutableList<ActivityLifecycleCallbacks> = mutableListOf()

        verify(exactly = 2) {
            application.registerActivityLifecycleCallbacks(
                capture(
                    callbacksCaptors,
                ),
            )
        }
        assertEquals(2, callbacksCaptors.size)

        // Closing
        visibleScreenTracker.close()

        verify { application.unregisterActivityLifecycleCallbacks(callbacksCaptors[0]) }
        verify { application.unregisterActivityLifecycleCallbacks(callbacksCaptors[1]) }
    }

    @Test
    fun activityLifecycle() {
        val visibleScreenTracker = this.visibleScreenService
        val activity = mockk<Activity>()

        assertEquals("unknown", visibleScreenTracker.currentlyVisibleScreen)

        visibleScreenTracker.activityResumed(activity)
        assertEquals(
            activity.javaClass.simpleName,
            visibleScreenTracker.currentlyVisibleScreen,
        )
        assertNull(visibleScreenTracker.previouslyVisibleScreen)

        visibleScreenTracker.activityPaused(activity)
        assertEquals("unknown", visibleScreenTracker.currentlyVisibleScreen)
        assertEquals(
            activity.javaClass.simpleName,
            visibleScreenTracker.previouslyVisibleScreen,
        )
    }

    @Test
    fun fragmentLifecycle() {
        val visibleScreenTracker = this.visibleScreenService
        val fragment = mockk<Fragment>()

        assertEquals("unknown", visibleScreenTracker.currentlyVisibleScreen)

        visibleScreenTracker.fragmentResumed(fragment)
        assertEquals(
            fragment.javaClass.simpleName,
            visibleScreenTracker.currentlyVisibleScreen,
        )
        assertNull(visibleScreenTracker.previouslyVisibleScreen)

        visibleScreenTracker.fragmentPaused(fragment)
        assertEquals("unknown", visibleScreenTracker.currentlyVisibleScreen)
        assertEquals(
            fragment.javaClass.simpleName,
            visibleScreenTracker.previouslyVisibleScreen,
        )
    }

    @Test
    fun fragmentLifecycle_navHostIgnored() {
        val visibleScreenTracker = this.visibleScreenService
        val fragment = mockk<Fragment>()
        val navHostFragment = mockk<NavHostFragment>()

        assertEquals("unknown", visibleScreenTracker.currentlyVisibleScreen)

        visibleScreenTracker.fragmentResumed(fragment)
        visibleScreenTracker.fragmentResumed(navHostFragment)
        assertEquals(
            fragment.javaClass.simpleName,
            visibleScreenTracker.currentlyVisibleScreen,
        )
        assertNull(visibleScreenTracker.previouslyVisibleScreen)

        visibleScreenTracker.fragmentPaused(navHostFragment)
        visibleScreenTracker.fragmentPaused(fragment)
        assertEquals("unknown", visibleScreenTracker.currentlyVisibleScreen)
        assertEquals(
            fragment.javaClass.simpleName,
            visibleScreenTracker.previouslyVisibleScreen,
        )
    }

    @Test
    fun fragmentLifecycle_dialogFragment() {
        val visibleScreenTracker = this.visibleScreenService
        val fragment = mockk<Fragment>()
        val dialogFragment = mockk<DialogFragment>()

        assertEquals("unknown", visibleScreenTracker.currentlyVisibleScreen)

        visibleScreenTracker.fragmentResumed(fragment)
        visibleScreenTracker.fragmentResumed(dialogFragment)
        assertEquals(
            dialogFragment.javaClass.simpleName,
            visibleScreenTracker.currentlyVisibleScreen,
        )
        assertEquals(
            fragment.javaClass.simpleName,
            visibleScreenTracker.previouslyVisibleScreen,
        )

        visibleScreenTracker.fragmentPaused(dialogFragment)
        assertEquals(
            fragment.javaClass.simpleName,
            visibleScreenTracker.currentlyVisibleScreen,
        )
        assertEquals(
            dialogFragment.javaClass.simpleName,
            visibleScreenTracker.previouslyVisibleScreen,
        )
    }

    @Test
    fun fragmentWinsOverActivityLifecycle() {
        val visibleScreenTracker = this.visibleScreenService
        val activity = mockk<Activity>()
        val fragment = mockk<Fragment>()

        assertEquals("unknown", visibleScreenTracker.currentlyVisibleScreen)

        visibleScreenTracker.activityResumed(activity)
        visibleScreenTracker.fragmentResumed(fragment)
        assertEquals(
            fragment.javaClass.simpleName,
            visibleScreenTracker.currentlyVisibleScreen,
        )
        assertNull(visibleScreenTracker.previouslyVisibleScreen)

        visibleScreenTracker.fragmentPaused(fragment)
        assertEquals(
            activity.javaClass.simpleName,
            visibleScreenTracker.currentlyVisibleScreen,
        )
        assertEquals(
            fragment.javaClass.simpleName,
            visibleScreenTracker.previouslyVisibleScreen,
        )
    }

    private val visibleScreenService: VisibleScreenTracker
        get() = VisibleScreenTrackerImpl(application)
}
