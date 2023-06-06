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

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.rum.internal.instrumentation.ScreenNameExtractor;
import io.opentelemetry.rum.internal.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.rum.internal.instrumentation.startup.AppStartupTimer;

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
