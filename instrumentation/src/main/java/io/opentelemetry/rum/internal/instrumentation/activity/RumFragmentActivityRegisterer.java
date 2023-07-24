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

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.opentelemetry.rum.internal.DefaultingActivityLifecycleCallbacks;

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
