/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import android.app.Activity;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VisibleScreenTrackerTest {
    private Application application;

    @BeforeEach
    void setUp() {
        application = mock();
    }

    @Test
    void activityLifecycle() {
        VisibleScreenTracker visibleScreenTracker = getVisibleScreenService();
        Activity activity = mock(Activity.class);

        assertEquals("unknown", visibleScreenTracker.getCurrentlyVisibleScreen());

        visibleScreenTracker.activityResumed(activity);
        assertEquals(
                activity.getClass().getSimpleName(),
                visibleScreenTracker.getCurrentlyVisibleScreen());
        assertNull(visibleScreenTracker.getPreviouslyVisibleScreen());

        visibleScreenTracker.activityPaused(activity);
        assertEquals("unknown", visibleScreenTracker.getCurrentlyVisibleScreen());
        assertEquals(
                activity.getClass().getSimpleName(),
                visibleScreenTracker.getPreviouslyVisibleScreen());
    }

    @Test
    void fragmentLifecycle() {
        VisibleScreenTracker visibleScreenTracker = getVisibleScreenService();
        Fragment fragment = mock(Fragment.class);

        assertEquals("unknown", visibleScreenTracker.getCurrentlyVisibleScreen());

        visibleScreenTracker.fragmentResumed(fragment);
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenTracker.getCurrentlyVisibleScreen());
        assertNull(visibleScreenTracker.getPreviouslyVisibleScreen());

        visibleScreenTracker.fragmentPaused(fragment);
        assertEquals("unknown", visibleScreenTracker.getCurrentlyVisibleScreen());
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenTracker.getPreviouslyVisibleScreen());
    }

    @Test
    void fragmentLifecycle_navHostIgnored() {
        VisibleScreenTracker visibleScreenTracker = getVisibleScreenService();
        Fragment fragment = mock(Fragment.class);
        NavHostFragment navHostFragment = mock(NavHostFragment.class);

        assertEquals("unknown", visibleScreenTracker.getCurrentlyVisibleScreen());

        visibleScreenTracker.fragmentResumed(fragment);
        visibleScreenTracker.fragmentResumed(navHostFragment);
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenTracker.getCurrentlyVisibleScreen());
        assertNull(visibleScreenTracker.getPreviouslyVisibleScreen());

        visibleScreenTracker.fragmentPaused(navHostFragment);
        visibleScreenTracker.fragmentPaused(fragment);
        assertEquals("unknown", visibleScreenTracker.getCurrentlyVisibleScreen());
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenTracker.getPreviouslyVisibleScreen());
    }

    @Test
    void fragmentLifecycle_dialogFragment() {
        VisibleScreenTracker visibleScreenTracker = getVisibleScreenService();
        Fragment fragment = mock(Fragment.class);
        DialogFragment dialogFragment = mock(DialogFragment.class);

        assertEquals("unknown", visibleScreenTracker.getCurrentlyVisibleScreen());

        visibleScreenTracker.fragmentResumed(fragment);
        visibleScreenTracker.fragmentResumed(dialogFragment);
        assertEquals(
                dialogFragment.getClass().getSimpleName(),
                visibleScreenTracker.getCurrentlyVisibleScreen());
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenTracker.getPreviouslyVisibleScreen());

        visibleScreenTracker.fragmentPaused(dialogFragment);
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenTracker.getCurrentlyVisibleScreen());
        assertEquals(
                dialogFragment.getClass().getSimpleName(),
                visibleScreenTracker.getPreviouslyVisibleScreen());
    }

    @Test
    void fragmentWinsOverActivityLifecycle() {
        VisibleScreenTracker visibleScreenTracker = getVisibleScreenService();
        Activity activity = mock(Activity.class);
        Fragment fragment = mock(Fragment.class);

        assertEquals("unknown", visibleScreenTracker.getCurrentlyVisibleScreen());

        visibleScreenTracker.activityResumed(activity);
        visibleScreenTracker.fragmentResumed(fragment);
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenTracker.getCurrentlyVisibleScreen());
        assertNull(visibleScreenTracker.getPreviouslyVisibleScreen());

        visibleScreenTracker.fragmentPaused(fragment);
        assertEquals(
                activity.getClass().getSimpleName(),
                visibleScreenTracker.getCurrentlyVisibleScreen());
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenTracker.getPreviouslyVisibleScreen());
    }

    private @NonNull VisibleScreenTracker getVisibleScreenService() {
        return new VisibleScreenTracker(application);
    }
}
