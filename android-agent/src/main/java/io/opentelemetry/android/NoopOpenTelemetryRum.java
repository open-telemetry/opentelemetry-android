/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.OpenTelemetrySdk;

enum NoopOpenTelemetryRum implements OpenTelemetryRum {
    INSTANCE;

    @Override
    public OpenTelemetrySdk getOpenTelemetry() {
        return OpenTelemetrySdk.builder().build();
    }

    @Override
    public String getRumSessionId() {
        // RUM session.id has the same format as traceId
        return TraceId.getInvalid();
    }
}
