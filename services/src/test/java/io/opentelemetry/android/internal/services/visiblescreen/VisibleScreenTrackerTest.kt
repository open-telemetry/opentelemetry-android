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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito

internal class VisibleScreenTrackerTest {
    private lateinit var application: Application

    @BeforeEach
    fun setUp() {
        application = Mockito.mock()
    }

    @Test
    fun verifyInitializationAndClose() {
        val visibleScreenTracker = this.visibleScreenService
        val callbacksCaptor =
            ArgumentCaptor.captor<ActivityLifecycleCallbacks?>()

        Mockito
            .verify(application, Mockito.times(2))
            .registerActivityLifecycleCallbacks(callbacksCaptor.capture())
        val callbacks = callbacksCaptor.getAllValues()
        assertEquals(2, callbacks.size)

        // Closing
        visibleScreenTracker.close()
        Mockito
            .verify(application)
            .unregisterActivityLifecycleCallbacks(callbacks[0])
        Mockito
            .verify(application)
            .unregisterActivityLifecycleCallbacks(callbacks[1])
    }

    @Test
    fun activityLifecycle() {
        val visibleScreenTracker = this.visibleScreenService
        val activity = Mockito.mock(Activity::class.java)

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
        val fragment = Mockito.mock(Fragment::class.java)

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
        val fragment = Mockito.mock(Fragment::class.java)
        val navHostFragment = Mockito.mock(NavHostFragment::class.java)

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
        val fragment = Mockito.mock(Fragment::class.java)
        val dialogFragment = Mockito.mock(DialogFragment::class.java)

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
        val activity = Mockito.mock(Activity::class.java)
        val fragment = Mockito.mock(Fragment::class.java)

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
