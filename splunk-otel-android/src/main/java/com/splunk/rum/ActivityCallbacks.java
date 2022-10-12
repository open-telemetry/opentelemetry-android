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

import static java.util.Objects.requireNonNull;

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

class ActivityCallbacks implements Application.ActivityLifecycleCallbacks {

    private final Map<String, ActivityTracer> tracersByActivityClassName = new HashMap<>();
    private final AtomicReference<String> initialAppActivity = new AtomicReference<>();
    private final Tracer tracer;
    private final VisibleScreenTracker visibleScreenTracker;
    private final AppStartupTimer startupTimer;
    private final List<AppStateListener> appStateListeners;
    // we count the number of activities that have been "started" and not yet "stopped" here to
    // figure out when the app goes into the background.
    private int numberOfOpenActivities = 0;

    private ActivityCallbacks(Builder builder) {
        this.tracer = requireNonNull(builder.tracer);
        this.visibleScreenTracker = requireNonNull(builder.visibleScreenTracker);
        this.startupTimer = requireNonNull(builder.startupTimer);
        this.appStateListeners = requireNonNull(builder.appStateListeners);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void onActivityPreCreated(
            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        getTracer(activity).startActivityCreation().addEvent("activityPreCreated");

        if (activity instanceof FragmentActivity) {
            FragmentManager fragmentManager =
                    ((FragmentActivity) activity).getSupportFragmentManager();
            fragmentManager.registerFragmentLifecycleCallbacks(
                    new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker), true);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        startupTimer.startUiInit();
        addEvent(activity, "activityCreated");
    }

    @Override
    public void onActivityPostCreated(
            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        addEvent(activity, "activityPostCreated");
    }

    @Override
    public void onActivityPreStarted(@NonNull Activity activity) {
        getTracer(activity)
                .initiateRestartSpanIfNecessary(tracersByActivityClassName.size() > 1)
                .addEvent("activityPreStarted");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (numberOfOpenActivities == 0) {
            for (AppStateListener appStateListener : appStateListeners) {
                appStateListener.appForegrounded();
            }
        }
        numberOfOpenActivities++;
        addEvent(activity, "activityStarted");
    }

    @Override
    public void onActivityPostStarted(@NonNull Activity activity) {
        addEvent(activity, "activityPostStarted");
    }

    @Override
    public void onActivityPreResumed(@NonNull Activity activity) {
        getTracer(activity).startSpanIfNoneInProgress("Resumed").addEvent("activityPreResumed");
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        addEvent(activity, "activityResumed");
    }

    @Override
    public void onActivityPostResumed(@NonNull Activity activity) {
        getTracer(activity)
                .addEvent("activityPostResumed")
                .addPreviousScreenAttribute()
                .endSpanForActivityResumed();
        visibleScreenTracker.activityResumed(activity);
    }

    @Override
    public void onActivityPrePaused(@NonNull Activity activity) {
        getTracer(activity).startSpanIfNoneInProgress("Paused").addEvent("activityPrePaused");
        visibleScreenTracker.activityPaused(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        addEvent(activity, "activityPaused");
    }

    @Override
    public void onActivityPostPaused(@NonNull Activity activity) {
        getTracer(activity).addEvent("activityPostPaused").endActiveSpan();
    }

    @Override
    public void onActivityPreStopped(@NonNull Activity activity) {
        getTracer(activity).startSpanIfNoneInProgress("Stopped").addEvent("activityPreStopped");
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (--numberOfOpenActivities == 0) {
            for (AppStateListener appStateListener : appStateListeners) {
                appStateListener.appBackgrounded();
            }
        }
        addEvent(activity, "activityStopped");
    }

    @Override
    public void onActivityPostStopped(@NonNull Activity activity) {
        getTracer(activity).addEvent("activityPostStopped").endActiveSpan();
    }

    @Override
    public void onActivityPreSaveInstanceState(
            @NonNull Activity activity, @NonNull Bundle outState) {
        // todo: add event
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // todo: add event
    }

    @Override
    public void onActivityPostSaveInstanceState(
            @NonNull Activity activity, @NonNull Bundle outState) {
        // todo: add event
    }

    @Override
    public void onActivityPreDestroyed(@NonNull Activity activity) {
        getTracer(activity).startSpanIfNoneInProgress("Destroyed").addEvent("activityPreDestroyed");
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        addEvent(activity, "activityDestroyed");
    }

    @Override
    public void onActivityPostDestroyed(@NonNull Activity activity) {
        getTracer(activity).addEvent("activityPostDestroyed").endActiveSpan();
    }

    private void addEvent(@NonNull Activity activity, String eventName) {
        getTracer(activity).addEvent(eventName);
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
                            startupTimer);
            tracersByActivityClassName.put(activity.getClass().getName(), activityTracer);
        }
        return activityTracer;
    }

    static class Builder {
        @Nullable private Tracer tracer;
        @Nullable private VisibleScreenTracker visibleScreenTracker;
        @Nullable private AppStartupTimer startupTimer;
        @Nullable private List<AppStateListener> appStateListeners;

        public ActivityCallbacks build() {
            return new ActivityCallbacks(this);
        }

        public Builder tracer(Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        public Builder visibleScreenTracker(VisibleScreenTracker visibleScreenTracker) {
            this.visibleScreenTracker = visibleScreenTracker;
            return this;
        }

        public Builder startupTimer(AppStartupTimer startupTimer) {
            this.startupTimer = startupTimer;
            return this;
        }

        public Builder appStateListeners(List<AppStateListener> appStateListeners) {
            this.appStateListeners = appStateListeners;
            return this;
        }
    }
}
