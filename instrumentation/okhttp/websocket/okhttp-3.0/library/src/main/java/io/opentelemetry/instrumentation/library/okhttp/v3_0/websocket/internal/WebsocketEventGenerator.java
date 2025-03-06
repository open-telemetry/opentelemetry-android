/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal;

import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder;
import io.opentelemetry.api.incubator.logs.ExtendedLogger;

public final class WebsocketEventGenerator {

    private WebsocketEventGenerator() {}

    private static final String SCOPE = "io.opentelemetry.websocket.events";

    private static ExtendedLogger logger =
            (ExtendedLogger) OpenTelemetry.noop().getLogsBridge().loggerBuilder(SCOPE).build();

    public static void configure(InstallationContext context) {
        WebsocketEventGenerator.logger =
                (ExtendedLogger)
                        context.getOpenTelemetry().getLogsBridge().loggerBuilder(SCOPE).build();
    }

    public static void generateEvent(String eventName, Attributes attributes) {
        ExtendedLogRecordBuilder logRecordBuilder = logger.logRecordBuilder();
        logRecordBuilder.setEventName(eventName).setAllAttributes(attributes).emit();
    }
}
