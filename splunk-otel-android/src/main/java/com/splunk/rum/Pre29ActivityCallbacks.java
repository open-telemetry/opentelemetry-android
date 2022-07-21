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
import io.opentelemetry.api.trace.Tracer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class Pre29ActivityCallbacks implements Application.ActivityLifecycleCallbacks {
    private final Tracer tracer;
    private final VisibleScreenTracker visibleScreenTracker;
    private final Map<String, ActivityTracer> tracersByActivityClassName = new HashMap<>();
    private final AtomicReference<String> initialAppActivity = new AtomicReference<>();
    private final AppStartupTimer appStartupTimer;
    private final List<AppStateListener> appStateListeners;
    private int numberOfOpenActivities = 0;

    Pre29ActivityCallbacks(
            Tracer tracer,
            VisibleScreenTracker visibleScreenTracker,
            AppStartupTimer appStartupTimer,
            List<AppStateListener> appStateListeners) {
        this.tracer = tracer;
        this.visibleScreenTracker = visibleScreenTracker;
        this.appStartupTimer = appStartupTimer;
        this.appStateListeners = appStateListeners;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        appStartupTimer.startUiInit();
        getTracer(activity).startActivityCreation().addEvent("activityCreated");

        if (activity instanceof FragmentActivity) {
            FragmentManager fragmentManager =
                    ((FragmentActivity) activity).getSupportFragmentManager();
            fragmentManager.registerFragmentLifecycleCallbacks(
                    new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker), true);
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (numberOfOpenActivities == 0) {
            for (AppStateListener appListener : appStateListeners) {
                appListener.appForegrounded();
            }
        }
        numberOfOpenActivities++;
        getTracer(activity)
                .initiateRestartSpanIfNecessary(tracersByActivityClassName.size() > 1)
                .addEvent("activityStarted");
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        getTracer(activity)
                .startSpanIfNoneInProgress("Resumed")
                .addEvent("activityResumed")
                .addPreviousScreenAttribute()
                .endSpanForActivityResumed();
        visibleScreenTracker.activityResumed(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        getTracer(activity)
                .startSpanIfNoneInProgress("Paused")
                .addEvent("activityPaused")
                .endActiveSpan();
        visibleScreenTracker.activityPaused(activity);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (--numberOfOpenActivities == 0) {
            for (AppStateListener appListener : appStateListeners) {
                appListener.appBackgrounded();
            }
        }
        getTracer(activity)
                .startSpanIfNoneInProgress("Stopped")
                .addEvent("activityStopped")
                .endActiveSpan();
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // todo: add event
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        getTracer(activity)
                .startSpanIfNoneInProgress("Destroyed")
                .addEvent("activityDestroyed")
                .endActiveSpan();
    }

    private ActivityTracer getTracer(Activity activity) {
        ActivityTracer activityTracer =
                tracersByActivityClassName.get(activity.getClass().getName());
        if (activityTracer == null) {
            activityTracer =
                    new ActivityTracer(
                            activity,
                            initialAppActivity,
                            tracer,
                            visibleScreenTracker,
                            appStartupTimer);
            tracersByActivityClassName.put(activity.getClass().getName(), activityTracer);
        }
        return activityTracer;
    }
}
