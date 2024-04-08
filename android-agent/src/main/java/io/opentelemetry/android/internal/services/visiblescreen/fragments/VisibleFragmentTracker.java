/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService;

public class VisibleFragmentTracker extends FragmentManager.FragmentLifecycleCallbacks {
    private final VisibleScreenService visibleScreenService;

    public VisibleFragmentTracker(VisibleScreenService visibleScreenService) {
        this.visibleScreenService = visibleScreenService;
    }

    @Override
    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        visibleScreenService.fragmentResumed(f);
    }

    @Override
    public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentPaused(fm, f);
        visibleScreenService.fragmentPaused(f);
    }
}
