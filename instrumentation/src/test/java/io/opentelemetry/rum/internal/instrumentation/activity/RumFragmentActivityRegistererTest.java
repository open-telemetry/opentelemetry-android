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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RumFragmentActivityRegistererTest {

    @Mock FragmentManager.FragmentLifecycleCallbacks fragmentCallbacks;

    @Test
    void createHappyPath() {
        FragmentActivity activity = mock(FragmentActivity.class);
        FragmentManager manager = mock(FragmentManager.class);

        when(activity.getSupportFragmentManager()).thenReturn(manager);

        Application.ActivityLifecycleCallbacks underTest =
                RumFragmentActivityRegisterer.create(fragmentCallbacks);

        underTest.onActivityPreCreated(activity, null);
        verify(manager).registerFragmentLifecycleCallbacks(fragmentCallbacks, true);
    }

    @Test
    void callbackIgnoresNonFragmentActivity() {
        Activity activity = mock(Activity.class);

        Application.ActivityLifecycleCallbacks underTest =
                RumFragmentActivityRegisterer.create(fragmentCallbacks);

        underTest.onActivityPreCreated(activity, null);
    }

    @Test
    void createPre29HappyPath() {
        FragmentActivity activity = mock(FragmentActivity.class);
        FragmentManager manager = mock(FragmentManager.class);

        when(activity.getSupportFragmentManager()).thenReturn(manager);

        Application.ActivityLifecycleCallbacks underTest =
                RumFragmentActivityRegisterer.createPre29(fragmentCallbacks);

        underTest.onActivityCreated(activity, null);
        verify(manager).registerFragmentLifecycleCallbacks(fragmentCallbacks, true);
    }

    @Test
    void pre29CallbackIgnoresNonFragmentActivity() {
        Activity activity = mock(Activity.class);

        Application.ActivityLifecycleCallbacks underTest =
                RumFragmentActivityRegisterer.createPre29(fragmentCallbacks);

        underTest.onActivityCreated(activity, null);
    }
}
