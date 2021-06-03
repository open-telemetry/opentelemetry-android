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
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.api.trace.Tracer;

class RumLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

    private final Map<String, NamedTrackableTracer> tracersByActivityClassName = new HashMap<>();
    private final AtomicBoolean appStartupComplete = new AtomicBoolean();
    private final Tracer tracer;
    private final VisibleScreenTracker visibleScreenTracker;

    RumLifecycleCallbacks(Tracer tracer, VisibleScreenTracker visibleScreenTracker) {
        this.tracer = tracer;
        this.visibleScreenTracker = visibleScreenTracker;
    }

    @Override
    public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        getOrCreateTracer(activity)
                .startTrackableCreation()
                .addEvent("activityPreCreated");

        if (activity instanceof FragmentActivity) {
            FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
            fragmentManager.registerFragmentLifecycleCallbacks(new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker), true);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        addEvent(activity, "activityCreated");
    }

    @Override
    public void onActivityPostCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        addEvent(activity, "activityPostCreated");
    }

    @Override
    public void onActivityPreStarted(@NonNull Activity activity) {
        getOrCreateTracer(activity)
                .startSpanIfNoneInProgress("Restarted")
                .addEvent("activityPreStarted");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        addEvent(activity, "activityStarted");
    }

    @Override
    public void onActivityPostStarted(@NonNull Activity activity) {
        addEvent(activity, "activityPostStarted");
    }

    @Override
    public void onActivityPreResumed(@NonNull Activity activity) {
        getOrCreateTracer(activity)
                .startSpanIfNoneInProgress("Resumed")
                .addEvent("activityPreResumed");
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        addEvent(activity, "activityResumed");
    }

    @Override
    public void onActivityPostResumed(@NonNull Activity activity) {
        getActivityTracer(activity)
                .addEvent("activityPostResumed")
                .endSpanForActivityResumed();
        visibleScreenTracker.activityResumed(activity);
    }

    @Override
    public void onActivityPrePaused(@NonNull Activity activity) {
        getOrCreateTracer(activity)
                .startSpanIfNoneInProgress("Paused")
                .addEvent("activityPrePaused");
        visibleScreenTracker.activityPaused(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        addEvent(activity, "activityPaused");
    }

    @Override
    public void onActivityPostPaused(@NonNull Activity activity) {
        getActivityTracer(activity).addEvent("activityPostPaused").endActiveSpan();
    }

    @Override
    public void onActivityPreStopped(@NonNull Activity activity) {
        getOrCreateTracer(activity)
                .startSpanIfNoneInProgress("Stopped")
                .addEvent("activityPreStopped");
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        addEvent(activity, "activityStopped");
    }

    @Override
    public void onActivityPostStopped(@NonNull Activity activity) {
        getActivityTracer(activity).addEvent("activityPostStopped").endActiveSpan();
    }

    @Override
    public void onActivityPreSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        //todo: add event
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        //todo: add event
    }

    @Override
    public void onActivityPostSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        //todo: add event
    }

    @Override
    public void onActivityPreDestroyed(@NonNull Activity activity) {
        getOrCreateTracer(activity)
                .startSpanIfNoneInProgress("Destroyed")
                .addEvent("activityPreDestroyed");
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        addEvent(activity, "activityDestroyed");
    }

    @Override
    public void onActivityPostDestroyed(@NonNull Activity activity) {
        getActivityTracer(activity).addEvent("activityPostDestroyed").endActiveSpan();
    }

    private void addEvent(@NonNull Activity activity, String eventName) {
        getActivityTracer(activity).addEvent(eventName);
    }

    private TrackableTracer getOrCreateTracer(Activity activity) {
        NamedTrackableTracer activityTracer = tracersByActivityClassName.get(activity.getClass().getName());
        if (activityTracer == null) {
            activityTracer = new NamedTrackableTracer(activity, appStartupComplete, tracer);
            tracersByActivityClassName.put(activity.getClass().getName(), activityTracer);
        }
        return activityTracer;
    }

    private TrackableTracer getActivityTracer(@NonNull Activity activity) {
        NamedTrackableTracer activityTracer = tracersByActivityClassName.get(activity.getClass().getName());
        if (activityTracer == null) {
            return TrackableTracer.NO_OP_TRACER;
        }
        return activityTracer;
    }

}
