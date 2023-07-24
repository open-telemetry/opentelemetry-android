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

package io.opentelemetry.rum.internal.instrumentation.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import io.opentelemetry.rum.internal.instrumentation.RumScreenName;
import io.opentelemetry.rum.internal.instrumentation.ScreenNameExtractor;
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
