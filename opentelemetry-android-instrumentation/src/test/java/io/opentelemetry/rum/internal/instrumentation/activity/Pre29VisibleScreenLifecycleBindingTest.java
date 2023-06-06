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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.app.Activity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Pre29VisibleScreenLifecycleBindingTest {
    @Mock Activity activity;
    @Mock VisibleScreenTracker tracker;

    @Test
    void postResumed() {
        Pre29VisibleScreenLifecycleBinding underTest =
                new Pre29VisibleScreenLifecycleBinding(tracker);
        underTest.onActivityResumed(activity);
        verify(tracker).activityResumed(activity);
        verifyNoMoreInteractions(tracker);
    }

    @Test
    void prePaused() {
        Pre29VisibleScreenLifecycleBinding underTest =
                new Pre29VisibleScreenLifecycleBinding(tracker);
        underTest.onActivityPaused(activity);
        verify(tracker).activityPaused(activity);
        verifyNoMoreInteractions(tracker);
    }
}
