/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;

final class OpenTelemetryRumImpl implements OpenTelemetryRum {

    private final OpenTelemetrySdk openTelemetrySdk;
    private final SessionId sessionId;

    OpenTelemetryRumImpl(OpenTelemetrySdk openTelemetrySdk, SessionId sessionId) {
        this.openTelemetrySdk = openTelemetrySdk;
        this.sessionId = sessionId;
    }

    @Override
    public OpenTelemetry getOpenTelemetry() {
        return openTelemetrySdk;
    }

    @Override
    public String getRumSessionId() {
        return sessionId.getSessionId();
    }
}
