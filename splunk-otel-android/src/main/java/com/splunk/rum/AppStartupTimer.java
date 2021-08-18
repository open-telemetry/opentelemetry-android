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

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

class AppStartupTimer {
    private final long firstPossibleTimestamp = System.currentTimeMillis();
    private volatile Span overallAppStartSpan = null;

    Span start(Tracer tracer) {
        //guard against a double-start and just return what's already in flight.
        if (overallAppStartSpan != null) {
            return overallAppStartSpan;
        }
        final Span appStart = tracer.spanBuilder("AppStart")
                .setStartTimestamp(firstPossibleTimestamp, TimeUnit.MILLISECONDS)
                .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_APPSTART)
                .setAttribute(SplunkRum.START_TYPE_KEY, "cold")
                .startSpan();
        overallAppStartSpan = appStart;
        return appStart;
    }

    void end() {
        if (overallAppStartSpan != null) {
            overallAppStartSpan.end();
            overallAppStartSpan = null;
        }
    }

    @Nullable
    Span getStartupSpan() {
        return overallAppStartSpan;
    }
}
