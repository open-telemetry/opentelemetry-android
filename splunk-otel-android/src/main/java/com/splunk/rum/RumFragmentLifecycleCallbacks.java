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

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.trace.Tracer;

import static com.splunk.rum.TrackableTracer.NO_OP_TRACER;

class RumFragmentLifecycleCallbacks extends FragmentManager.FragmentLifecycleCallbacks {
    private final Map<String, NamedTrackableTracer> tracersByFragmentClassName = new HashMap<>();

    private final Tracer tracer;
    private final VisibleScreenTracker visibleScreenTracker;

    RumFragmentLifecycleCallbacks(Tracer tracer, VisibleScreenTracker visibleScreenTracker) {
        this.tracer = tracer;
        this.visibleScreenTracker = visibleScreenTracker;
    }

    @Override
    public void onFragmentPreAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
        super.onFragmentPreAttached(fm, f, context);
        getOrCreateTracer(f)
                .startTrackableCreation()
                .addEvent("fragmentPreAttached");
    }

    @Override
    public void onFragmentAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
        super.onFragmentAttached(fm, f, context);
        addEvent(f, "fragmentAttached");
    }

    @Override
    public void onFragmentPreCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
        super.onFragmentPreCreated(fm, f, savedInstanceState);
        addEvent(f, "fragmentPreCreated");
    }

    @Override
    public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
        super.onFragmentCreated(fm, f, savedInstanceState);
        addEvent(f, "fragmentCreated");
    }

    @Override
    public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onFragmentViewCreated(fm, f, v, savedInstanceState);
        getOrCreateTracer(f)
                .startSpanIfNoneInProgress("Restored")
                .addEvent("fragmentViewCreated");
    }

    @Override
    public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentStarted(fm, f);
        addEvent(f, "fragmentStarted");
    }

    @Override
    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentResumed(fm, f);
        getOrCreateTracer(f)
                .startSpanIfNoneInProgress("Resumed")
                .addEvent("fragmentResumed")
                .endActiveSpan();
        visibleScreenTracker.fragmentResumed(f);
    }

    @Override
    public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentPaused(fm, f);
        visibleScreenTracker.fragmentPaused(f);
        getOrCreateTracer(f).startSpanIfNoneInProgress("Paused").addEvent("fragmentPaused");
    }

    @Override
    public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentStopped(fm, f);
        getFragmentTracer(f).addEvent("fragmentStopped").endActiveSpan();
    }

    @Override
    public void onFragmentSaveInstanceState(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Bundle outState) {
        super.onFragmentSaveInstanceState(fm, f, outState);
    }

    @Override
    public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentViewDestroyed(fm, f);
        getOrCreateTracer(f)
                .startSpanIfNoneInProgress("ViewDestroyed")
                .addEvent("fragmentViewDestroyed")
                .endActiveSpan();
    }

    @Override
    public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentDestroyed(fm, f);
        //note: this might not get called if the dev has checked "retainInstance" on the fragment
        getOrCreateTracer(f)
                .startSpanIfNoneInProgress("Destroyed")
                .addEvent("fragmentDestroyed");
    }

    @Override
    public void onFragmentDetached(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentDetached(fm, f);
        // this is a terminal operation, but might also be the only thing we see on app getting killed, so
        getOrCreateTracer(f)
                .startSpanIfNoneInProgress("Detached")
                .addEvent("fragmentDetached")
                .endActiveSpan();
    }

    private void addEvent(@NonNull Fragment fragment, String eventName) {
        getFragmentTracer(fragment).addEvent(eventName);
    }

    private TrackableTracer getOrCreateTracer(Fragment fragment) {
        NamedTrackableTracer activityTracer = tracersByFragmentClassName.get(fragment.getClass().getName());
        if (activityTracer == null) {
            activityTracer = new NamedTrackableTracer(fragment, tracer);
            tracersByFragmentClassName.put(fragment.getClass().getName(), activityTracer);
        }
        return activityTracer;
    }

    private TrackableTracer getFragmentTracer(@NonNull Fragment fragment) {
        NamedTrackableTracer activityTracer = tracersByFragmentClassName.get(fragment.getClass().getName());
        if (activityTracer == null) {
            return NO_OP_TRACER;
        }
        return activityTracer;
    }

}
