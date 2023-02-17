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

package com.splunk.rum;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.opentelemetry.rum.internal.DefaultingActivityLifecycleCallbacks;

class ActivityCallbacks implements DefaultingActivityLifecycleCallbacks {

    private final ActivityTracerCache tracers;

    ActivityCallbacks(ActivityTracerCache tracers) {
        this.tracers = tracers;
    }

    @Override
    public void onActivityPreCreated(
            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        tracers.startActivityCreation(activity).addEvent("activityPreCreated");
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        tracers.addEvent(activity, "activityCreated");
    }

    @Override
    public void onActivityPostCreated(
            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        tracers.addEvent(activity, "activityPostCreated");
    }

    @Override
    public void onActivityPreStarted(@NonNull Activity activity) {
        tracers.initiateRestartSpanIfNecessary(activity).addEvent("activityPreStarted");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityStarted");
    }

    @Override
    public void onActivityPostStarted(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPostStarted");
    }

    @Override
    public void onActivityPreResumed(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Resumed").addEvent("activityPreResumed");
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityResumed");
    }

    @Override
    public void onActivityPostResumed(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPostResumed")
                .addPreviousScreenAttribute()
                .endSpanForActivityResumed();
    }

    @Override
    public void onActivityPrePaused(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Paused").addEvent("activityPrePaused");
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPaused");
    }

    @Override
    public void onActivityPostPaused(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPostPaused").endActiveSpan();
    }

    @Override
    public void onActivityPreStopped(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Stopped").addEvent("activityPreStopped");
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityStopped");
    }

    @Override
    public void onActivityPostStopped(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPostStopped").endActiveSpan();
    }

    @Override
    public void onActivityPreDestroyed(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Destroyed").addEvent("activityPreDestroyed");
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityDestroyed");
    }

    @Override
    public void onActivityPostDestroyed(@NonNull Activity activity) {
        tracers.addEvent(activity, "activityPostDestroyed").endActiveSpan();
    }
}
