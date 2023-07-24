/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.lifecycle;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.android.instrumentation.ScreenNameExtractor;
import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.android.instrumentation.startup.AppStartupTimer;
import java.util.function.Function;

public class AndroidLifecycleInstrumentationBuilder {
    ScreenNameExtractor screenNameExtractor = ScreenNameExtractor.DEFAULT;
    AppStartupTimer startupTimer;
    VisibleScreenTracker visibleScreenTracker;
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
        return new AndroidLifecycleInstrumentation(this);
    }
}
