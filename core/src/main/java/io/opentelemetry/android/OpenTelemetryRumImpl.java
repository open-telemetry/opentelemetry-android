/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.android.session.SessionManager;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder;
import io.opentelemetry.api.incubator.logs.ExtendedLogger;
import io.opentelemetry.sdk.OpenTelemetrySdk;

final class OpenTelemetryRumImpl implements OpenTelemetryRum {

    private final OpenTelemetrySdk openTelemetrySdk;
    private final SessionManager sessionManager;

    private final ExtendedLogger logger;

    OpenTelemetryRumImpl(OpenTelemetrySdk openTelemetrySdk, SessionManager sessionManager) {
        this.openTelemetrySdk = openTelemetrySdk;
        this.sessionManager = sessionManager;
        this.logger =
                (ExtendedLogger)
                        openTelemetrySdk
                                .getLogsBridge()
                                .loggerBuilder("io.opentelemetry.rum.events")
                                .build();
    }

    @Override
    public OpenTelemetry getOpenTelemetry() {
        return openTelemetrySdk;
    }

    @Override
    public String getRumSessionId() {
        return sessionManager.getSessionId();
    }

    @Override
    public void emitEvent(String eventName, String body, Attributes attributes) {
        ExtendedLogRecordBuilder logRecordBuilder = logger.logRecordBuilder();
        logRecordBuilder.setEventName(eventName).setBody(body).setAllAttributes(attributes).emit();
    }
}
