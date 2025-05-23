/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.android.session.SessionProvider;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder;
import io.opentelemetry.api.incubator.logs.ExtendedLogger;
import io.opentelemetry.sdk.OpenTelemetrySdk;

final class OpenTelemetryRumImpl implements OpenTelemetryRum {

    private final OpenTelemetrySdk openTelemetrySdk;
    private final SessionProvider sessionProvider;

    private final ExtendedLogger logger;

    OpenTelemetryRumImpl(OpenTelemetrySdk openTelemetrySdk, SessionProvider sessionProvider) {
        this.openTelemetrySdk = openTelemetrySdk;
        this.sessionProvider = sessionProvider;
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
        return sessionProvider.getSessionId();
    }

    @Override
    public void emitEvent(String eventName, String body, Attributes attributes) {
        ExtendedLogRecordBuilder logRecordBuilder = logger.logRecordBuilder();
        logRecordBuilder.setEventName(eventName).setBody(body).setAllAttributes(attributes).emit();
    }
}
