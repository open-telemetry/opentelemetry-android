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
                .setAttribute(SplunkRum.SCREEN_NAME_KEY, trackableName)
                .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_UI)
                .startSpan();
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
