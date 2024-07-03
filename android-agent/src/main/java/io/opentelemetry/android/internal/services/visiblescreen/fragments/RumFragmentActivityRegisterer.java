/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.fragments;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks;

/**
 * Registers the RumFragmentLifecycleCallbacks when an activity is created. There are just 2 factory
 * methods here, one for API level before 29, and one for the rest.
 */
public class RumFragmentActivityRegisterer {

    private RumFragmentActivityRegisterer() {}

    public static Application.ActivityLifecycleCallbacks create(
            FragmentManager.FragmentLifecycleCallbacks fragmentCallbacks) {
        return new DefaultingActivityLifecycleCallbacks() {
            @Override
            public void onActivityPreCreated(
                    @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity instanceof FragmentActivity) {
                    register((FragmentActivity) activity, fragmentCallbacks);
                }
            }
        };
    }

    public static Application.ActivityLifecycleCallbacks createPre29(
            FragmentManager.FragmentLifecycleCallbacks fragmentCallbacks) {
        return new DefaultingActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(
                    @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity instanceof FragmentActivity) {
                    register((FragmentActivity) activity, fragmentCallbacks);
                }
            }
        };
    }

    private static void register(
            FragmentActivity activity,
            FragmentManager.FragmentLifecycleCallbacks fragmentCallbacks) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        fragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true);
    }
}
