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

class VisibleScreenServiceTest {
    private Application application;

    @BeforeEach
    void setUp() {
        application = mock();
    }

    @Test
    void activityLifecycle() {
        VisibleScreenService visibleScreenService = getVisibleScreenService();
        Activity activity = mock(Activity.class);

        assertEquals("unknown", visibleScreenService.getCurrentlyVisibleScreen());

        visibleScreenService.activityResumed(activity);
        assertEquals(
                activity.getClass().getSimpleName(),
                visibleScreenService.getCurrentlyVisibleScreen());
        assertNull(visibleScreenService.getPreviouslyVisibleScreen());

        visibleScreenService.activityPaused(activity);
        assertEquals("unknown", visibleScreenService.getCurrentlyVisibleScreen());
        assertEquals(
                activity.getClass().getSimpleName(),
                visibleScreenService.getPreviouslyVisibleScreen());
    }

    @Test
    void fragmentLifecycle() {
        VisibleScreenService visibleScreenService = getVisibleScreenService();
        Fragment fragment = mock(Fragment.class);

        assertEquals("unknown", visibleScreenService.getCurrentlyVisibleScreen());

        visibleScreenService.fragmentResumed(fragment);
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenService.getCurrentlyVisibleScreen());
        assertNull(visibleScreenService.getPreviouslyVisibleScreen());

        visibleScreenService.fragmentPaused(fragment);
        assertEquals("unknown", visibleScreenService.getCurrentlyVisibleScreen());
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenService.getPreviouslyVisibleScreen());
    }

    @Test
    void fragmentLifecycle_navHostIgnored() {
        VisibleScreenService visibleScreenService = getVisibleScreenService();
        Fragment fragment = mock(Fragment.class);
        NavHostFragment navHostFragment = mock(NavHostFragment.class);

        assertEquals("unknown", visibleScreenService.getCurrentlyVisibleScreen());

        visibleScreenService.fragmentResumed(fragment);
        visibleScreenService.fragmentResumed(navHostFragment);
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenService.getCurrentlyVisibleScreen());
        assertNull(visibleScreenService.getPreviouslyVisibleScreen());

        visibleScreenService.fragmentPaused(navHostFragment);
        visibleScreenService.fragmentPaused(fragment);
        assertEquals("unknown", visibleScreenService.getCurrentlyVisibleScreen());
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenService.getPreviouslyVisibleScreen());
    }

    @Test
    void fragmentLifecycle_dialogFragment() {
        VisibleScreenService visibleScreenService = getVisibleScreenService();
        Fragment fragment = mock(Fragment.class);
        DialogFragment dialogFragment = mock(DialogFragment.class);

        assertEquals("unknown", visibleScreenService.getCurrentlyVisibleScreen());

        visibleScreenService.fragmentResumed(fragment);
        visibleScreenService.fragmentResumed(dialogFragment);
        assertEquals(
                dialogFragment.getClass().getSimpleName(),
                visibleScreenService.getCurrentlyVisibleScreen());
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenService.getPreviouslyVisibleScreen());

        visibleScreenService.fragmentPaused(dialogFragment);
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenService.getCurrentlyVisibleScreen());
        assertEquals(
                dialogFragment.getClass().getSimpleName(),
                visibleScreenService.getPreviouslyVisibleScreen());
    }

    @Test
    void fragmentWinsOverActivityLifecycle() {
        VisibleScreenService visibleScreenService = getVisibleScreenService();
        Activity activity = mock(Activity.class);
        Fragment fragment = mock(Fragment.class);

        assertEquals("unknown", visibleScreenService.getCurrentlyVisibleScreen());

        visibleScreenService.activityResumed(activity);
        visibleScreenService.fragmentResumed(fragment);
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenService.getCurrentlyVisibleScreen());
        assertNull(visibleScreenService.getPreviouslyVisibleScreen());

        visibleScreenService.fragmentPaused(fragment);
        assertEquals(
                activity.getClass().getSimpleName(),
                visibleScreenService.getCurrentlyVisibleScreen());
        assertEquals(
                fragment.getClass().getSimpleName(),
                visibleScreenService.getPreviouslyVisibleScreen());
    }

    private @NonNull VisibleScreenService getVisibleScreenService() {
        return new VisibleScreenService(application);
    }
}
