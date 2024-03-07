/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.lifecycle;

import io.opentelemetry.android.instrumentation.ScreenNameExtractor;
import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.android.instrumentation.startup.AppStartupTimer;
import io.opentelemetry.api.trace.Tracer;
import java.util.function.Function;

public class AndroidLifecycleInstrumentationBuilder {
    private static final VisibleScreenTracker INVALID_SCREEN_TRACKER = new VisibleScreenTracker();
    private static final AppStartupTimer INVALID_TIMER = new AppStartupTimer();
    ScreenNameExtractor screenNameExtractor = ScreenNameExtractor.DEFAULT;
    AppStartupTimer startupTimer = INVALID_TIMER;
    VisibleScreenTracker visibleScreenTracker = INVALID_SCREEN_TRACKER;
    Function<Tracer, Tracer> tracerCustomizer = Function.identity();

    public AndroidLifecycleInstrumentationBuilder setStartupTimer(AppStartupTimer timer) {
        this.startupTimer = timer;
        return this;
    }

    public AndroidLifecycleInstrumentationBuilder setVisibleScreenTracker(
            VisibleScreenTracker tracker) {
        this.visibleScreenTracker = tracker;
        return this;
    }

    public AndroidLifecycleInstrumentationBuilder setTracerCustomizer(
            Function<Tracer, Tracer> customizer) {
        this.tracerCustomizer = customizer;
        return this;
    }

    public AndroidLifecycleInstrumentationBuilder setScreenNameExtractor(
            ScreenNameExtractor screenNameExtractor) {
        this.screenNameExtractor = screenNameExtractor;
        return this;
    }

    public AndroidLifecycleInstrumentation build() {
        if (visibleScreenTracker == INVALID_SCREEN_TRACKER) {
            throw new IllegalStateException("visibleScreenTracker must be configured.");
        }
        if (startupTimer == INVALID_TIMER) {
            throw new IllegalStateException("startupTimer must be configured.");
        }
        return new AndroidLifecycleInstrumentation(this);
    }
}
