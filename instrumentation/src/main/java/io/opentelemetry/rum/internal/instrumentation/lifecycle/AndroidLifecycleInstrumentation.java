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

package io.opentelemetry.rum.internal.instrumentation.lifecycle;

import android.app.Application;
import android.os.Build;
import androidx.annotation.NonNull;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.rum.internal.instrumentation.InstrumentedApplication;
import io.opentelemetry.rum.internal.instrumentation.ScreenNameExtractor;
import io.opentelemetry.rum.internal.instrumentation.activity.ActivityCallbacks;
import io.opentelemetry.rum.internal.instrumentation.activity.ActivityTracerCache;
import io.opentelemetry.rum.internal.instrumentation.activity.Pre29ActivityCallbacks;
import io.opentelemetry.rum.internal.instrumentation.activity.Pre29VisibleScreenLifecycleBinding;
import io.opentelemetry.rum.internal.instrumentation.activity.RumFragmentActivityRegisterer;
import io.opentelemetry.rum.internal.instrumentation.activity.VisibleScreenLifecycleBinding;
import io.opentelemetry.rum.internal.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.rum.internal.instrumentation.fragment.RumFragmentLifecycleCallbacks;
import io.opentelemetry.rum.internal.instrumentation.startup.AppStartupTimer;
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
