/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
