/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.android.session.SessionManager;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkEventLoggerProvider;
import java.util.Map;

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

    /**
     * Emits a custom event with the provided event name, body, and attributes.
     *
     * @param eventName The name of the event to emit.
     * @param body The body content of the event (optional).
     * @param attributes Additional attributes for the event (optional).
     */
    @Override
    public void emitEvent(String eventName, String body, Map<String, String> attributes) {
        // Use the OpenTelemetry SDK to emit the event
        SdkEventLoggerProvider.create(getOpenTelemetry().logsBridge())
                .get("default-scope") // A scope can be dynamically assigned if needed
                .builder(eventName)
                .put("body", body)
                .putAll(attributes)
                .emit();
    }
}
