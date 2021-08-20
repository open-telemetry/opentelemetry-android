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

import java.util.function.Supplier;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

class ActiveSpan {
    private final VisibleScreenTracker visibleScreenTracker;

    private Span span;
    private Scope scope;

    ActiveSpan(VisibleScreenTracker visibleScreenTracker) {
        this.visibleScreenTracker = visibleScreenTracker;
    }

    boolean spanInProgress() {
        return span != null;
    }

    void startSpan(Supplier<Span> spanCreator) {
        //don't start one if there's already one in progress
        if (span != null) {
            return;
        }
        this.span = spanCreator.get();
        scope = span.makeCurrent();
    }

    void endActiveSpan() {
        if (scope != null) {
            scope.close();
            scope = null;
        }
        if (this.span != null) {
            this.span.end();
            this.span = null;
        }
    }

    void addEvent(String eventName) {
        if (span != null) {
            span.addEvent(eventName);
        }
    }

    void addPreviousScreenAttribute(String screenName) {
        if (span == null) {
            return;
        }
        String previouslyVisibleScreen = visibleScreenTracker.getPreviouslyVisibleScreen();
        if (!screenName.equals(previouslyVisibleScreen)) {
            span.setAttribute(SplunkRum.LAST_SCREEN_NAME_KEY, previouslyVisibleScreen);
        }
    }
}
