/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.rum.internal.instrumentation.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import android.app.Activity;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import org.junit.jupiter.api.Test;

class VisibleScreenTrackerTest {

    @Test
    void activityLifecycle() {
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();
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
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();
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
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();
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
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();
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
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();
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
}
