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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.concurrent.atomic.AtomicReference;

class ActivityTracer {
    static final AttributeKey<String> ACTIVITY_NAME_KEY = AttributeKey.stringKey("activityName");
    static final String APP_START_SPAN_NAME = "AppStart";

    private final AtomicReference<String> initialAppActivity;
    private final Tracer tracer;
    private final String activityName;
    private final String screenName;
    private final AppStartupTimer appStartupTimer;
    private final ActiveSpan activeSpan;

    ActivityTracer(
            Activity activity,
            AtomicReference<String> initialAppActivity,
            Tracer tracer,
            VisibleScreenTracker visibleScreenTracker,
            AppStartupTimer appStartupTimer) {
        this.initialAppActivity = initialAppActivity;
        this.tracer = tracer;
        this.activityName = activity.getClass().getSimpleName();
        RumScreenName rumScreenName = activity.getClass().getAnnotation(RumScreenName.class);
        this.screenName = rumScreenName == null ? activityName : rumScreenName.value();
        this.appStartupTimer = appStartupTimer;
        this.activeSpan = new ActiveSpan(visibleScreenTracker);
    }

    ActivityTracer startSpanIfNoneInProgress(String action) {
        if (activeSpan.spanInProgress()) {
            return this;
        }
        activeSpan.startSpan(() -> createSpan(action));
        return this;
    }

    ActivityTracer startActivityCreation() {
        activeSpan.startSpan(this::makeCreationSpan);
        return this;
    }

    private Span makeCreationSpan() {
        // If the application has never loaded an activity, or this is the initial activity getting
        // re-created,
        // we name this span specially to show that it's the application starting up. Otherwise, use
        // the activity class name as the base of the span name.
        boolean isColdStart = initialAppActivity.get() == null;
        if (isColdStart) {
            return createSpanWithParent("Created", appStartupTimer.getStartupSpan());
        }
        if (activityName.equals(initialAppActivity.get())) {
            return createAppStartSpan("warm");
        }
        return createSpan("Created");
    }

    ActivityTracer initiateRestartSpanIfNecessary(boolean multiActivityApp) {
        if (activeSpan.spanInProgress()) {
            return this;
        }
        activeSpan.startSpan(() -> makeRestartSpan(multiActivityApp));
        return this;
    }

    @NonNull
    private Span makeRestartSpan(boolean multiActivityApp) {
        // restarting the first activity is a "hot" AppStart
        // Note: in a multi-activity application, navigating back to the first activity can trigger
        // this, so it would not be ideal to call it an AppStart.
        if (!multiActivityApp && activityName.equals(initialAppActivity.get())) {
            return createAppStartSpan("hot");
        }
        return createSpan("Restarted");
    }

    private Span createAppStartSpan(String startType) {
        Span span = createSpan(APP_START_SPAN_NAME);
        span.setAttribute(SplunkRum.START_TYPE_KEY, startType);
        // override the component to be appstart
        span.setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_APPSTART);
        return span;
    }

    private Span createSpan(String spanName) {
        return createSpanWithParent(spanName, null);
    }

    private Span createSpanWithParent(String spanName, @Nullable Span parentSpan) {
        final SpanBuilder spanBuilder =
                tracer.spanBuilder(spanName)
                        .setAttribute(ACTIVITY_NAME_KEY, activityName)
                        .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_UI);
        if (parentSpan != null) {
            spanBuilder.setParent(parentSpan.storeInContext(Context.current()));
        }
        Span span = spanBuilder.startSpan();
        // do this after the span is started, so we can override the default screen.name set by the
        // RumAttributeAppender.
        span.setAttribute(SplunkRum.SCREEN_NAME_KEY, screenName);
        return span;
    }

    void endSpanForActivityResumed() {
        if (initialAppActivity.get() == null) {
            initialAppActivity.set(activityName);
        }
        endActiveSpan();
    }

    void endActiveSpan() {
        // If we happen to be in app startup, make sure this ends it. It's harmless if we're already
        // out of the startup phase.
        appStartupTimer.end();
        activeSpan.endActiveSpan();
    }

    ActivityTracer addPreviousScreenAttribute() {
        activeSpan.addPreviousScreenAttribute(activityName);
        return this;
    }

    ActivityTracer addEvent(String eventName) {
        activeSpan.addEvent(eventName);
        return this;
    }
}
