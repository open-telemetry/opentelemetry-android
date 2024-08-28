/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.session.SessionProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

final class SessionIdSpanAppender implements SpanProcessor {

    private final SessionProvider sessionProvider;

    public SessionIdSpanAppender(SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        span.setAttribute(RumConstants.SESSION_ID_KEY, sessionProvider.getSessionId());
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {}

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
