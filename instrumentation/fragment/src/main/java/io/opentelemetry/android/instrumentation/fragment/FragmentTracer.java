/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment;

import androidx.fragment.app.Fragment;
import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.instrumentation.common.ActiveSpan;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

class FragmentTracer {
    static final AttributeKey<String> FRAGMENT_NAME_KEY = AttributeKey.stringKey("fragment.name");

    private final String fragmentName;
    private final String screenName;
    private final Tracer tracer;
    private final ActiveSpan activeSpan;

    private FragmentTracer(Builder builder) {
        this.tracer = builder.tracer;
        this.fragmentName = builder.getFragmentName();
        this.screenName = builder.screenName;
        this.activeSpan = builder.activeSpan;
    }

    FragmentTracer startSpanIfNoneInProgress(String action) {
        if (activeSpan.spanInProgress()) {
            return this;
        }
        activeSpan.startSpan(() -> createSpan(action));
        return this;
    }

    FragmentTracer startFragmentCreation() {
        activeSpan.startSpan(() -> createSpan("Created"));
        return this;
    }

    private Span createSpan(String spanName) {
        Span span =
                tracer.spanBuilder(spanName)
                        .setAttribute(FRAGMENT_NAME_KEY, fragmentName)
                        .startSpan();
        // do this after the span is started, so we can override the default screen.name set by the
        // RumAttributeAppender.
        span.setAttribute(RumConstants.SCREEN_NAME_KEY, screenName);
        return span;
    }

    void endActiveSpan() {
        activeSpan.endActiveSpan();
    }

    FragmentTracer addPreviousScreenAttribute() {
        activeSpan.addPreviousScreenAttribute(fragmentName);
        return this;
    }

    FragmentTracer addEvent(String eventName) {
        activeSpan.addEvent(eventName);
        return this;
    }

    static Builder builder(Fragment fragment) {
        return new Builder(fragment);
    }

    static class Builder {
        private static final ActiveSpan INVALID_ACTIVE_SPAN = new ActiveSpan(() -> null);
        private static final Tracer INVALID_TRACER = spanName -> null;
        private final Fragment fragment;
        public String screenName = "";
        private Tracer tracer = INVALID_TRACER;
        private ActiveSpan activeSpan = INVALID_ACTIVE_SPAN;

        public Builder(Fragment fragment) {
            this.fragment = fragment;
        }

        Builder setTracer(Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        public Builder setScreenName(String screenName) {
            this.screenName = screenName;
            return this;
        }

        Builder setActiveSpan(ActiveSpan activeSpan) {
            this.activeSpan = activeSpan;
            return this;
        }

        public String getFragmentName() {
            return fragment.getClass().getSimpleName();
        }

        FragmentTracer build() {
            if (activeSpan == INVALID_ACTIVE_SPAN) {
                throw new IllegalStateException("activeSpan must be configured.");
            }
            if (tracer == INVALID_TRACER) {
                throw new IllegalStateException("tracer must be configured.");
            }
            return new FragmentTracer(this);
        }
    }
}
