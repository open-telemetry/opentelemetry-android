/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal.instrumentation.fragment;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

class FragmentCallbackTestHarness {

    private final RumFragmentLifecycleCallbacks callbacks;

    FragmentCallbackTestHarness(RumFragmentLifecycleCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    void runFragmentCreationLifecycle(Fragment fragment) {
        Context context = mock(Context.class);
        FragmentManager fragmentManager = mock(FragmentManager.class);
        Bundle bundle = mock(Bundle.class);

        callbacks.onFragmentPreAttached(fragmentManager, fragment, context);
        callbacks.onFragmentAttached(fragmentManager, fragment, context);
        callbacks.onFragmentPreCreated(fragmentManager, fragment, bundle);
        callbacks.onFragmentCreated(fragmentManager, fragment, bundle);
        runFragmentRestoredLifecycle(fragment);
    }

    void runFragmentRestoredLifecycle(Fragment fragment) {
        FragmentManager fragmentManager = mock(FragmentManager.class);
        Bundle bundle = mock(Bundle.class);
        View view = mock(View.class);
        callbacks.onFragmentViewCreated(fragmentManager, fragment, view, bundle);
        callbacks.onFragmentStarted(fragmentManager, fragment);
        callbacks.onFragmentResumed(fragmentManager, fragment);
    }

    void runFragmentResumedLifecycle(Fragment fragment) {
        FragmentManager fragmentManager = mock(FragmentManager.class);
        callbacks.onFragmentResumed(fragmentManager, fragment);
    }

    void runFragmentPausedLifecycle(Fragment fragment) {
        FragmentManager fragmentManager = mock(FragmentManager.class);
        callbacks.onFragmentPaused(fragmentManager, fragment);
        callbacks.onFragmentStopped(fragmentManager, fragment);
    }

    void runFragmentDetachedFromActiveLifecycle(Fragment fragment) {
        FragmentManager fragmentManager = mock(FragmentManager.class);

        runFragmentPausedLifecycle(fragment);
        callbacks.onFragmentViewDestroyed(fragmentManager, fragment);
        callbacks.onFragmentDestroyed(fragmentManager, fragment);
        runFragmentDetachedLifecycle(fragment);
    }

    void runFragmentViewDestroyedFromStoppedLifecycle(Fragment fragment) {
        FragmentManager fragmentManager = mock(FragmentManager.class);

        callbacks.onFragmentViewDestroyed(fragmentManager, fragment);
    }

    void runFragmentDetachedFromStoppedLifecycle(Fragment fragment) {
        FragmentManager fragmentManager = mock(FragmentManager.class);

        runFragmentViewDestroyedFromStoppedLifecycle(fragment);
        callbacks.onFragmentDestroyed(fragmentManager, fragment);
        runFragmentDetachedLifecycle(fragment);
    }

    void runFragmentDetachedLifecycle(Fragment fragment) {
        FragmentManager fragmentManager = mock(FragmentManager.class);

        callbacks.onFragmentDetached(fragmentManager, fragment);
    }
}
