/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.android.session.SessionManager;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;

final class OpenTelemetryRumImpl implements OpenTelemetryRum {

    private final OpenTelemetrySdk openTelemetrySdk;
    private final SessionManager sessionManager;

    OpenTelemetryRumImpl(OpenTelemetrySdk openTelemetrySdk, SessionManager sessionManager) {
        this.openTelemetrySdk = openTelemetrySdk;
        this.sessionManager = sessionManager;
    }

    @Override
    public OpenTelemetry getOpenTelemetry() {
        return openTelemetrySdk;
    }

    @Override
    public String getRumSessionId() {
        return sessionManager.getSessionId();
    }
}
