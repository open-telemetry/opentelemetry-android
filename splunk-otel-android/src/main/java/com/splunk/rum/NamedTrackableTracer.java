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

import android.app.Activity;

import androidx.fragment.app.Fragment;

import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

class NamedTrackableTracer implements TrackableTracer {
    static final AttributeKey<String> ACTIVITY_NAME_KEY = AttributeKey.stringKey("activityName");
    static final AttributeKey<String> FRAGMENT_NAME_KEY = AttributeKey.stringKey("fragmentName");

    private final AtomicBoolean appStartupComplete;
    private final Tracer tracer;
    private final String trackableName;
    private final AttributeKey<String> nameKey;

    private Span span;
    private Scope scope;

    NamedTrackableTracer(Activity activity, AtomicBoolean appStartupComplete, Tracer tracer) {
        this.appStartupComplete = appStartupComplete;
        this.tracer = tracer;
        this.trackableName = activity.getClass().getSimpleName();
        this.nameKey = ACTIVITY_NAME_KEY;
    }

    NamedTrackableTracer(Fragment activity, Tracer tracer) {
        this.appStartupComplete = new AtomicBoolean(true);
        this.tracer = tracer;
        this.trackableName = activity.getClass().getSimpleName();
        this.nameKey = FRAGMENT_NAME_KEY;
    }

    @Override
    public TrackableTracer startSpanIfNoneInProgress(String action) {
        if (span != null) {
            return this;
        }
        startSpan(trackableName + " " + action);
        return this;
    }

    @Override
    public TrackableTracer startTrackableCreation() {
        String spanName;
        //If the application has never loaded an activity, we name this span specially to show that
        //it's the application starting up. Otherwise, use the activity class name as the base of the span name.
        if (!appStartupComplete.get()) {
            spanName = "AppStart";
        } else {
            spanName = trackableName + " Created";
        }
        startSpan(spanName);
        return this;
    }

    private void startSpan(String spanName) {
        span = tracer.spanBuilder(spanName)
                .setAttribute(nameKey, trackableName)
                .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_UI)
                .startSpan();
        //do this after the span is started, so we can override the default screen.name set by the RumAttributeAppender.
        span.setAttribute(SplunkRum.SCREEN_NAME_KEY, trackableName);
        scope = span.makeCurrent();
    }

    @Override
    public void endSpanForActivityResumed() {
        appStartupComplete.set(true);
        endActiveSpan();
    }

    @Override
    public void endActiveSpan() {
        if (scope != null) {
            scope.close();
            scope = null;
        }
        if (span != null) {
            span.end();
            span = null;
        }
    }

    @Override
    public TrackableTracer addEvent(String eventName) {
        if (span != null) {
            span.addEvent(eventName);
        }
        return this;
    }
}
