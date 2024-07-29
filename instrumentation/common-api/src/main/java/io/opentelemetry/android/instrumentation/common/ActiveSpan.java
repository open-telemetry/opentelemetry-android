/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common;

import androidx.annotation.Nullable;
import io.opentelemetry.android.common.RumConstants;
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
            span.setAttribute(RumConstants.LAST_SCREEN_NAME_KEY, previouslyVisibleScreen);
        }
    }
}
