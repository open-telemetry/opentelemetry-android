package com.splunk.rum;

import android.app.Activity;

import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

class ActivityTracer {
    static final AttributeKey<String> ACTIVITY_NAME_KEY = AttributeKey.stringKey("activityName");

    private final Activity activity;
    private final AtomicBoolean appStartupComplete;
    private final Tracer tracer;

    private Span span;
    private Scope scope;

    ActivityTracer(Activity activity, AtomicBoolean appStartupComplete, Tracer tracer) {
        this.activity = activity;
        this.appStartupComplete = appStartupComplete;
        this.tracer = tracer;
    }

    void startActivityCreation() {
        String spanName;
        //If the application has never loaded an activity, we name this span specially to show that
        //it's the application starting up. Otherwise, use the activity class name as the span name.
        if (!appStartupComplete.get()) {
            spanName = "App Startup";
        } else {
            spanName = activity.getClass().getSimpleName();
        }
        span = tracer.spanBuilder(spanName)
                .setAttribute(ACTIVITY_NAME_KEY, activity.getClass().getSimpleName())
                .startSpan();
        scope = span.makeCurrent();
    }

    void endActivityCreation() {
        appStartupComplete.set(true);
        if (scope != null) {
            scope.close();
            scope = null;
        }
        if (span != null) {
            span.end();
            span = null;
        }
    }

    void addEvent(String eventName) {
        if (span != null) {
            span.addEvent(eventName);
        }
    }
}
