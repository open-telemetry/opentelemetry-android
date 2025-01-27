/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity;

import static io.opentelemetry.android.common.RumConstants.APP_START_SPAN_NAME;
import static io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY;
import static io.opentelemetry.android.common.RumConstants.START_TYPE_KEY;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer;
import io.opentelemetry.android.instrumentation.common.ActiveSpan;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityTracer {
    static final AttributeKey<String> ACTIVITY_NAME_KEY = AttributeKey.stringKey("activityName");

    private final AtomicReference<String> initialAppActivity;
    private final Tracer tracer;
    private final String activityName;
    private final String screenName;
    private final AppStartupTimer appStartupTimer;
    private final ActiveSpan activeSpan;

    private ActivityTracer(Builder builder) {
        this.initialAppActivity = builder.initialAppActivity;
        this.tracer = builder.tracer;
        this.activityName = builder.getActivityName();
        this.screenName = builder.screenName;
        this.appStartupTimer = builder.appStartupTimer;
        this.activeSpan = builder.activeSpan;
    }

    ActivityTracer startSpanIfNoneInProgress(String spanName) {
        if (activeSpan.spanInProgress()) {
            return this;
        }
        activeSpan.startSpan(() -> createSpan(spanName));
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
        span.setAttribute(START_TYPE_KEY, startType);
        return span;
    }

    private Span createSpan(String spanName) {
        return createSpanWithParent(spanName, null);
    }

    private Span createSpanWithParent(String spanName, @Nullable Span parentSpan) {
        final SpanBuilder spanBuilder =
                tracer.spanBuilder(spanName).setAttribute(ACTIVITY_NAME_KEY, activityName);
        if (parentSpan != null) {
            spanBuilder.setParent(parentSpan.storeInContext(Context.current()));
        }
        Span span = spanBuilder.startSpan();
        // do this after the span is started, so we can override the default screen.name set by the
        // RumAttributeAppender.
        span.setAttribute(SCREEN_NAME_KEY, screenName);
        return span;
    }

    public void endSpanForActivityResumed() {
        if (initialAppActivity.get() == null) {
            initialAppActivity.set(activityName);
        }
        endActiveSpan();
    }

    public void endActiveSpan() {
        // If we happen to be in app startup, make sure this ends it. It's harmless if we're already
        // out of the startup phase.
        appStartupTimer.end();
        activeSpan.endActiveSpan();
    }

    public ActivityTracer addPreviousScreenAttribute() {
        activeSpan.addPreviousScreenAttribute(activityName);
        return this;
    }

    public ActivityTracer addEvent(String eventName) {
        activeSpan.addEvent(eventName);
        return this;
    }

    static Builder builder(Activity activity) {
        return new Builder(activity);
    }

    static class Builder {
        private static final ActiveSpan INVALID_ACTIVE_SPAN = new ActiveSpan(() -> null);
        private static final Tracer INVALID_TRACER = spanName -> null;
        private static final AppStartupTimer INVALID_TIMER = new AppStartupTimer();
        private final Activity activity;
        public String screenName = "unknown_screen";
        private AtomicReference<String> initialAppActivity = new AtomicReference<>();
        private Tracer tracer = INVALID_TRACER;
        private AppStartupTimer appStartupTimer = INVALID_TIMER;
        private ActiveSpan activeSpan = INVALID_ACTIVE_SPAN;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        public Builder setVisibleScreenTracker(VisibleScreenTracker visibleScreenTracker) {
            this.activeSpan = new ActiveSpan(visibleScreenTracker::getPreviouslyVisibleScreen);
            return this;
        }

        public Builder setInitialAppActivity(String activityName) {
            initialAppActivity.set(activityName);
            return this;
        }

        public Builder setInitialAppActivity(AtomicReference<String> initialAppActivity) {
            this.initialAppActivity = initialAppActivity;
            return this;
        }

        public Builder setTracer(Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        public Builder setAppStartupTimer(AppStartupTimer appStartupTimer) {
            this.appStartupTimer = appStartupTimer;
            return this;
        }

        public Builder setActiveSpan(ActiveSpan activeSpan) {
            this.activeSpan = activeSpan;
            return this;
        }

        private String getActivityName() {
            return activity.getClass().getSimpleName();
        }

        public Builder setScreenName(String screenName) {
            this.screenName = screenName;
            return this;
        }

        public ActivityTracer build() {
            if (activeSpan == INVALID_ACTIVE_SPAN) {
                throw new IllegalStateException("activeSpan must be configured.");
            }
            if (tracer == INVALID_TRACER) {
                throw new IllegalStateException("tracer must be configured.");
            }
            if (appStartupTimer == INVALID_TIMER) {
                throw new IllegalStateException("appStartupTimer must be configured.");
            }
            return new ActivityTracer(this);
        }
    }
}
