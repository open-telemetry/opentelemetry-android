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

package io.opentelemetry.rum.internal.util;

import static io.opentelemetry.rum.internal.RumConstants.LAST_SCREEN_NAME_KEY;

import androidx.annotation.Nullable;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import java.util.function.Supplier;

public class ActiveSpan {
    private final Supplier<String> lastVisibleScreen;

    @Nullable private Span span;
    @Nullable private Scope scope;

    public ActiveSpan(Supplier<String> lastVisibleScreen) {
        this.lastVisibleScreen = lastVisibleScreen;
    }

    public boolean spanInProgress() {
        return span != null;
    }

    // it's fine to not close the scope here, will be closed in endActiveSpan()
    @SuppressWarnings("MustBeClosedChecker")
    public void startSpan(Supplier<Span> spanCreator) {
        // don't start one if there's already one in progress
        if (span != null) {
            return;
        }
        this.span = spanCreator.get();
        scope = span.makeCurrent();
    }

    public void endActiveSpan() {
        if (scope != null) {
            scope.close();
            scope = null;
        }
        if (this.span != null) {
            this.span.end();
            this.span = null;
        }
    }

    public void addEvent(String eventName) {
        if (span != null) {
            span.addEvent(eventName);
        }
    }

    public void addPreviousScreenAttribute(String screenName) {
        if (span == null) {
            return;
        }
        String previouslyVisibleScreen = lastVisibleScreen.get();
        if (previouslyVisibleScreen != null && !screenName.equals(previouslyVisibleScreen)) {
            span.setAttribute(LAST_SCREEN_NAME_KEY, previouslyVisibleScreen);
        }
    }
}
