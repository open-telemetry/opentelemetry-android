/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.api.trace.Tracer;

final class SessionIdChangeTracer implements SessionIdChangeListener {

    private final Tracer tracer;

    SessionIdChangeTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void onChange(String oldSessionId, String newSessionId) {
        tracer.spanBuilder("sessionId.change")
                .setAttribute(RumConstants.PREVIOUS_SESSION_ID_KEY, oldSessionId)
                .startSpan()
                .end();
    }
}
