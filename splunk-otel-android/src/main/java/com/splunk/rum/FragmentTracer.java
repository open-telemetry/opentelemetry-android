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

import androidx.fragment.app.Fragment;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

class FragmentTracer {
    static final AttributeKey<String> FRAGMENT_NAME_KEY = AttributeKey.stringKey("fragmentName");

    private final String fragmentName;
    private final String screenName;
    private final Tracer tracer;
    private final ActiveSpan activeSpan;

    FragmentTracer(Fragment fragment, Tracer tracer, VisibleScreenTracker visibleScreenTracker) {
        this.tracer = tracer;
        this.fragmentName = fragment.getClass().getSimpleName();
        RumScreenName rumScreenName = fragment.getClass().getAnnotation(RumScreenName.class);
        this.screenName = rumScreenName == null ? fragmentName : rumScreenName.value();
        this.activeSpan = new ActiveSpan(visibleScreenTracker);
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
                        .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_UI)
                        .startSpan();
        // do this after the span is started, so we can override the default screen.name set by the
        // RumAttributeAppender.
        span.setAttribute(SplunkRum.SCREEN_NAME_KEY, screenName);
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
}
