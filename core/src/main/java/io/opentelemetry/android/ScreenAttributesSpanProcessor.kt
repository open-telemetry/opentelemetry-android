/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY;

import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public final class ScreenAttributesSpanProcessor implements SpanProcessor {

    private final VisibleScreenTracker visibleScreenTracker;

    public ScreenAttributesSpanProcessor(VisibleScreenTracker visibleScreenTracker) {
        this.visibleScreenTracker = visibleScreenTracker;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        String currentScreen = visibleScreenTracker.getCurrentlyVisibleScreen();
        span.setAttribute(SCREEN_NAME_KEY, currentScreen);
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        // nop
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
