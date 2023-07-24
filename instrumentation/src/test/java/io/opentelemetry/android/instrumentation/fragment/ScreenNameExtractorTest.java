/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import io.opentelemetry.android.instrumentation.RumScreenName;
import io.opentelemetry.android.instrumentation.ScreenNameExtractor;
import org.junit.jupiter.api.Test;

class ScreenNameExtractorTest {

    @Test
    void testActivity() {
        Activity activity = new Activity();
        String name = ScreenNameExtractor.DEFAULT.extract(activity);
        assertEquals("Activity", name);
    }

    @Test
    void testFragment() {
        Fragment fragment = new Fragment();
        String name = ScreenNameExtractor.DEFAULT.extract(fragment);
        assertEquals("Fragment", name);
    }

    @Test
    void testAnnotatedActivity() {
        Activity activity = new AnnotatedActivity();
        String name = ScreenNameExtractor.DEFAULT.extract(activity);
        assertEquals("squarely", name);
    }

    @Test
    void testAnnotatedFragment() {
        Fragment fragment = new AnnotatedFragment();
        String name = ScreenNameExtractor.DEFAULT.extract(fragment);
        assertEquals("bumpity", name);
    }

    @RumScreenName("bumpity")
    static class AnnotatedFragment extends Fragment {}

    @RumScreenName("squarely")
    static class AnnotatedActivity extends Activity {}
}
