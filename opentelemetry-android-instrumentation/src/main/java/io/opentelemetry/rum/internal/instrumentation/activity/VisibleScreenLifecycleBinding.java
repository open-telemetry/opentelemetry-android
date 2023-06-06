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

import androidx.annotation.NonNull;

import io.opentelemetry.rum.internal.DefaultingActivityLifecycleCallbacks;

/**
 * An ActivityLifecycleCallbacks that is responsible for telling the VisibleScreenTracker when an
 * activity has been resumed and when an activity has been paused. It's just a glue class.
 */
public class VisibleScreenLifecycleBinding implements DefaultingActivityLifecycleCallbacks {

    private final VisibleScreenTracker visibleScreenTracker;

    public VisibleScreenLifecycleBinding(VisibleScreenTracker visibleScreenTracker) {
        this.visibleScreenTracker = visibleScreenTracker;
    }

    @Override
    public void onActivityPostResumed(@NonNull Activity activity) {
        visibleScreenTracker.activityResumed(activity);
    }

    @Override
    public void onActivityPrePaused(@NonNull Activity activity) {
        visibleScreenTracker.activityPaused(activity);
    }
}
