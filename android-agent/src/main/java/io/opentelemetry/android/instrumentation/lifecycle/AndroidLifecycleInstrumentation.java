/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.lifecycle;

import android.app.Application;
import android.os.Build;
import androidx.annotation.NonNull;
import io.opentelemetry.android.instrumentation.activity.ActivityCallbacks;
import io.opentelemetry.android.instrumentation.activity.ActivityTracerCache;
import io.opentelemetry.android.instrumentation.activity.Pre29ActivityCallbacks;
import io.opentelemetry.android.instrumentation.activity.Pre29VisibleScreenLifecycleBinding;
import io.opentelemetry.android.instrumentation.activity.RumFragmentActivityRegisterer;
import io.opentelemetry.android.instrumentation.activity.VisibleScreenLifecycleBinding;
import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.android.instrumentation.common.InstrumentedApplication;
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor;
import io.opentelemetry.android.instrumentation.fragment.RumFragmentLifecycleCallbacks;
import io.opentelemetry.android.instrumentation.startup.AppStartupTimer;
import io.opentelemetry.api.trace.Tracer;
import java.util.function.Function;

/**
 * This is an umbrella instrumentation that covers several things: * startup timer callback is
 * registered so that UI startup time can be measured - activity lifecycle callbacks are registered
 * so that lifecycle events can be generated - activity lifecycle callback listener is registered to
 * that will register a FragmentLifecycleCallbacks when appropriate - activity lifecycle callback
 * listener is registered to dispatch events to the VisibleScreenTracker
 */
public class AndroidLifecycleInstrumentation {

    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.lifecycle";
    private final AppStartupTimer startupTimer;
    private final VisibleScreenTracker visibleScreenTracker;

    private final Function<Tracer, Tracer> tracerCustomizer;
    private final ScreenNameExtractor screenNameExtractor;

    AndroidLifecycleInstrumentation(AndroidLifecycleInstrumentationBuilder builder) {
        this.startupTimer = builder.startupTimer;
        this.visibleScreenTracker = builder.visibleScreenTracker;
        this.tracerCustomizer = builder.tracerCustomizer;
        this.screenNameExtractor = builder.screenNameExtractor;
    }

    public static AndroidLifecycleInstrumentationBuilder builder() {
        return new AndroidLifecycleInstrumentationBuilder();
    }

    public void installOn(InstrumentedApplication app) {
        installStartupTimerInstrumentation(app);
        installActivityLifecycleEventsInstrumentation(app);
        installFragmentLifecycleInstrumentation(app);
        installScreenTrackingInstrumentation(app);
    }

    private void installStartupTimerInstrumentation(InstrumentedApplication app) {
        app.getApplication()
                .registerActivityLifecycleCallbacks(startupTimer.createLifecycleCallback());
    }

    private void installActivityLifecycleEventsInstrumentation(InstrumentedApplication app) {
        Application.ActivityLifecycleCallbacks activityCallbacks = buildActivityEventsCallback(app);
        app.getApplication().registerActivityLifecycleCallbacks(activityCallbacks);
    }

    @NonNull
    private Application.ActivityLifecycleCallbacks buildActivityEventsCallback(
            InstrumentedApplication instrumentedApp) {
        Tracer delegateTracer =
                instrumentedApp.getOpenTelemetrySdk().getTracer(INSTRUMENTATION_SCOPE);
        Tracer tracer = tracerCustomizer.apply(delegateTracer);

        ActivityTracerCache tracers =
                new ActivityTracerCache(
                        tracer, visibleScreenTracker, startupTimer, screenNameExtractor);
        if (Build.VERSION.SDK_INT < 29) {
            return new Pre29ActivityCallbacks(tracers);
        }
        return new ActivityCallbacks(tracers);
    }

    private void installFragmentLifecycleInstrumentation(InstrumentedApplication app) {
        Application.ActivityLifecycleCallbacks fragmentRegisterer = buildFragmentRegisterer(app);
        app.getApplication().registerActivityLifecycleCallbacks(fragmentRegisterer);
    }

    @NonNull
    private Application.ActivityLifecycleCallbacks buildFragmentRegisterer(
            InstrumentedApplication app) {

        Tracer delegateTracer = app.getOpenTelemetrySdk().getTracer(INSTRUMENTATION_SCOPE);
        Tracer tracer = tracerCustomizer.apply(delegateTracer);
        RumFragmentLifecycleCallbacks fragmentLifecycle =
                new RumFragmentLifecycleCallbacks(
                        tracer, visibleScreenTracker, screenNameExtractor);
        if (Build.VERSION.SDK_INT < 29) {
            return RumFragmentActivityRegisterer.createPre29(fragmentLifecycle);
        }
        return RumFragmentActivityRegisterer.create(fragmentLifecycle);
    }

    private void installScreenTrackingInstrumentation(InstrumentedApplication app) {
        Application.ActivityLifecycleCallbacks screenTrackingBinding =
                buildScreenTrackingBinding(visibleScreenTracker);
        app.getApplication().registerActivityLifecycleCallbacks(screenTrackingBinding);
    }

    @NonNull
    private Application.ActivityLifecycleCallbacks buildScreenTrackingBinding(
            VisibleScreenTracker visibleScreenTracker) {
        if (Build.VERSION.SDK_INT < 29) {
            return new Pre29VisibleScreenLifecycleBinding(visibleScreenTracker);
        }
        return new VisibleScreenLifecycleBinding(visibleScreenTracker);
    }
}
