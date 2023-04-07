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

import static com.splunk.rum.SplunkRum.COMPONENT_APPSTART;
import static com.splunk.rum.SplunkRum.COMPONENT_KEY;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.rum.internal.instrumentation.startup.AppStartupTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class InitializationEvents {
    private final AppStartupTimer startupTimer;
    private final List<Event> events = new ArrayList<>();
    private long startTimeNanos = -1;

    InitializationEvents(AppStartupTimer startupTimer) {
        this.startupTimer = startupTimer;
    }

    void begin() {
        startTimeNanos = startupTimer.clockNow();
    }

    void emit(String eventName) {
        events.add(new Event(eventName, startupTimer.clockNow()));
    }

    void recordInitializationSpans(ConfigFlags flags, Tracer delegateTracer) {
        Tracer tracer =
                spanName ->
                        delegateTracer
                                .spanBuilder(spanName)
                                .setAttribute(COMPONENT_KEY, COMPONENT_APPSTART);

        Span overallAppStart = startupTimer.start(tracer);
        Span span =
                tracer.spanBuilder("SplunkRum.initialize")
                        .setParent(Context.current().with(overallAppStart))
                        .setStartTimestamp(startTimeNanos, TimeUnit.NANOSECONDS)
                        .startSpan();

        span.setAttribute("config_settings", flags.toString());

        for (Event initializationEvent : events) {
            span.addEvent(initializationEvent.name, initializationEvent.time, TimeUnit.NANOSECONDS);
        }
        long spanEndTime = startupTimer.clockNow();
        // we only want to create SplunkRum.initialize span when there is a AppStart span so we
        // register a callback that is called right before AppStart span is ended
        startupTimer.setCompletionCallback(() -> span.end(spanEndTime, TimeUnit.NANOSECONDS));
    }

    private static class Event {
        private final String name;
        private final long time;

        private Event(String name, long time) {
            this.name = name;
            this.time = time;
        }
    }
}
