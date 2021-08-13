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

import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

class NamedTrackableTracer implements TrackableTracer {
    static final AttributeKey<String> ACTIVITY_NAME_KEY = AttributeKey.stringKey("activityName");
    static final AttributeKey<String> FRAGMENT_NAME_KEY = AttributeKey.stringKey("fragmentName");
    static final AttributeKey<String> START_TYPE_KEY = AttributeKey.stringKey("start.type");
    static final String APP_START_SPAN_NAME = "AppStart";

    private final AtomicReference<String> initialAppActivity;
    private final Tracer tracer;
    private final String trackableName;
    private final VisibleScreenTracker visibleScreenTracker;
    private final AttributeKey<String> nameKey;

    private Span span;
    private Scope scope;

    NamedTrackableTracer(Activity activity, AtomicReference<String> initialAppActivity, Tracer tracer, VisibleScreenTracker visibleScreenTracker) {
        this.initialAppActivity = initialAppActivity;
        this.tracer = tracer;
        this.trackableName = activity.getClass().getSimpleName();
        this.visibleScreenTracker = visibleScreenTracker;
        this.nameKey = ACTIVITY_NAME_KEY;
    }

    NamedTrackableTracer(Fragment fragment, Tracer tracer, VisibleScreenTracker visibleScreenTracker) {
        this.initialAppActivity = new AtomicReference<>("not relevant for fragments");
        this.tracer = tracer;
        this.trackableName = fragment.getClass().getSimpleName();
        this.visibleScreenTracker = visibleScreenTracker;
        this.nameKey = FRAGMENT_NAME_KEY;
    }

    @Override
    public TrackableTracer startSpanIfNoneInProgress(String action) {
        if (span != null) {
            return this;
        }
        startSpan(action);
        return this;
    }

    @Override
    public TrackableTracer startTrackableCreation() {
        //If the application has never loaded an activity, or this is the initial activity getting re-created,
        // we name this span specially to show that it's the application starting up. Otherwise, use
        // the activity class name as the base of the span name.
        if (initialAppActivity.get() == null || trackableName.equals(initialAppActivity.get())) {
            Span span = startSpan(APP_START_SPAN_NAME);
            span.setAttribute(START_TYPE_KEY, initialAppActivity.get() == null ? "cold" : "warm");
        } else {
            startSpan("Created");
        }
        return this;
    }

    @Override
    public TrackableTracer initiateRestartSpanIfNecessary(boolean multiActivityApp) {
        if (span != null) {
            return this;
        }
        //restarting the first activity is a "hot" AppStart
        //Note: in a multi-activity application, navigating back to the first activity can trigger
        //this, so it would not be ideal to call it an AppStart.
        if (!multiActivityApp && trackableName.equals(initialAppActivity.get())) {
            Span span = startSpan(APP_START_SPAN_NAME);
            span.setAttribute(START_TYPE_KEY, "hot");
        } else {
            startSpan("Restarted");
        }
        return this;
    }

    private Span startSpan(String spanName) {
        span = tracer.spanBuilder(spanName)
                .setAttribute(nameKey, trackableName)
                .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_UI)
                .startSpan();
        //do this after the span is started, so we can override the default screen.name set by the RumAttributeAppender.
        span.setAttribute(SplunkRum.SCREEN_NAME_KEY, trackableName);
        scope = span.makeCurrent();
        return span;
    }

    @Override
    public void endSpanForActivityResumed() {
        if (initialAppActivity.get() == null) {
            initialAppActivity.set(trackableName);
        }
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
    public TrackableTracer addPreviousScreenAttribute() {
        String previouslyVisibleScreen = visibleScreenTracker.getPreviouslyVisibleScreen();
        if (!trackableName.equals(previouslyVisibleScreen)) {
            span.setAttribute(SplunkRum.LAST_SCREEN_NAME_KEY, previouslyVisibleScreen);
        }
        return this;
    }

    @Override
    public TrackableTracer addEvent(String eventName) {
        if (span != null) {
            span.addEvent(eventName);
        }
        return this;
    }
}
