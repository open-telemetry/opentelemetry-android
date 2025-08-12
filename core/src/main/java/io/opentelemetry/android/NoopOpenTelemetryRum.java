/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TraceId;

enum NoopOpenTelemetryRum implements OpenTelemetryRum {
    INSTANCE;

    @Override
    public OpenTelemetry getOpenTelemetry() {
        return OpenTelemetry.noop();
    }

    @Override
    public String getRumSessionId() {
        // RUM session.id has the same format as traceId
        return TraceId.getInvalid();
    }

    @Override
    public void emitEvent(String eventName, String body, Attributes attributes) {}

    @Override
    public void shutdown() {
        // nop
    }
}
