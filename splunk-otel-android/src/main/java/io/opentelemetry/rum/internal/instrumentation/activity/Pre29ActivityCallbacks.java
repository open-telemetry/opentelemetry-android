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
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.opentelemetry.rum.internal.DefaultingActivityLifecycleCallbacks;

public class Pre29ActivityCallbacks implements DefaultingActivityLifecycleCallbacks {
    private final ActivityTracerCache tracers;

    public Pre29ActivityCallbacks(ActivityTracerCache tracers) {
        this.tracers = tracers;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        tracers.startActivityCreation(activity).addEvent("activityCreated");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        tracers.initiateRestartSpanIfNecessary(activity).addEvent("activityStarted");
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Resumed")
                .addEvent("activityResumed")
                .addPreviousScreenAttribute()
                .endSpanForActivityResumed();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Paused")
                .addEvent("activityPaused")
                .endActiveSpan();
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Stopped")
                .addEvent("activityStopped")
                .endActiveSpan();
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        tracers.startSpanIfNoneInProgress(activity, "Destroyed")
                .addEvent("activityDestroyed")
                .endActiveSpan();
    }
}
