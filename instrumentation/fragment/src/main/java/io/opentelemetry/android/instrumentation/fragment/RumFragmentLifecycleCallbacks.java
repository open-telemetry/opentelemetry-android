/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import io.opentelemetry.android.instrumentation.common.ActiveSpan;
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor;
import io.opentelemetry.api.trace.Tracer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RumFragmentLifecycleCallbacks extends FragmentManager.FragmentLifecycleCallbacks {
    private final Map<String, FragmentTracer> tracersByFragmentClassName = new HashMap<>();

    private final Tracer tracer;
    private final Supplier<String> lastVisibleScreen;
    private final ScreenNameExtractor screenNameExtractor;

    public RumFragmentLifecycleCallbacks(
            Tracer tracer,
            Supplier<String> lastVisibleScreen,
            ScreenNameExtractor screenNameExtractor) {
        this.tracer = tracer;
        this.lastVisibleScreen = lastVisibleScreen;
        this.screenNameExtractor = screenNameExtractor;
    }

    @Override
    public void onFragmentPreAttached(
            @NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
        super.onFragmentPreAttached(fm, f, context);
        getTracer(f).startFragmentCreation().addEvent("fragmentPreAttached");
    }

    @Override
    public void onFragmentAttached(
            @NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
        super.onFragmentAttached(fm, f, context);
        addEvent(f, "fragmentAttached");
    }

    @Override
    public void onFragmentPreCreated(
            @NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
        super.onFragmentPreCreated(fm, f, savedInstanceState);
        addEvent(f, "fragmentPreCreated");
    }

    @Override
    public void onFragmentCreated(
            @NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
        super.onFragmentCreated(fm, f, savedInstanceState);
        addEvent(f, "fragmentCreated");
    }

    @Override
    public void onFragmentViewCreated(
            @NonNull FragmentManager fm,
            @NonNull Fragment f,
            @NonNull View v,
            @Nullable Bundle savedInstanceState) {
        super.onFragmentViewCreated(fm, f, v, savedInstanceState);
        getTracer(f).startSpanIfNoneInProgress("Restored").addEvent("fragmentViewCreated");
    }

    @Override
    public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentStarted(fm, f);
        addEvent(f, "fragmentStarted");
    }

    @Override
    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentResumed(fm, f);
        getTracer(f)
                .startSpanIfNoneInProgress("Resumed")
                .addEvent("fragmentResumed")
                .addPreviousScreenAttribute()
                .endActiveSpan();
    }

    @Override
    public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentPaused(fm, f);
        getTracer(f).startSpanIfNoneInProgress("Paused").addEvent("fragmentPaused").endActiveSpan();
    }

    @Override
    public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentStopped(fm, f);
        getTracer(f)
                .startSpanIfNoneInProgress("Stopped")
                .addEvent("fragmentStopped")
                .endActiveSpan();
    }

    @Override
    public void onFragmentSaveInstanceState(
            @NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Bundle outState) {
        super.onFragmentSaveInstanceState(fm, f, outState);
    }

    @Override
    public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentViewDestroyed(fm, f);
        getTracer(f)
                .startSpanIfNoneInProgress("ViewDestroyed")
                .addEvent("fragmentViewDestroyed")
                .endActiveSpan();
    }

    @Override
    public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentDestroyed(fm, f);
        // note: this might not get called if the dev has checked "retainInstance" on the fragment
        getTracer(f).startSpanIfNoneInProgress("Destroyed").addEvent("fragmentDestroyed");
    }

    @Override
    public void onFragmentDetached(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentDetached(fm, f);
        // this is a terminal operation, but might also be the only thing we see on app getting
        // killed, so
        getTracer(f)
                .startSpanIfNoneInProgress("Detached")
                .addEvent("fragmentDetached")
                .endActiveSpan();
    }

    private void addEvent(@NonNull Fragment fragment, String eventName) {
        FragmentTracer fragmentTracer =
                tracersByFragmentClassName.get(fragment.getClass().getName());
        if (fragmentTracer != null) {
            fragmentTracer.addEvent(eventName);
        }
    }

    private FragmentTracer getTracer(Fragment fragment) {
        FragmentTracer activityTracer =
                tracersByFragmentClassName.get(fragment.getClass().getName());
        if (activityTracer == null) {
            activityTracer =
                    FragmentTracer.builder(fragment)
                            .setTracer(tracer)
                            .setScreenName(screenNameExtractor.extract(fragment))
                            .setActiveSpan(new ActiveSpan(lastVisibleScreen))
                            .build();
            tracersByFragmentClassName.put(fragment.getClass().getName(), activityTracer);
        }
        return activityTracer;
    }
}
