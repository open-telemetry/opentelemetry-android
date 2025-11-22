/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal;

import io.opentelemetry.android.annotations.Incubating;
import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.android.ktx.SessionExtensionsKt;
import io.opentelemetry.android.session.SessionProvider;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;

public final class WebsocketEventGenerator {

    private WebsocketEventGenerator() {}

    private static final String SCOPE = "io.opentelemetry.websocket.events";

    private static Logger logger =
            OpenTelemetry.noop().getLogsBridge().loggerBuilder(SCOPE).build();

    private static SessionProvider sessionProvider = SessionProvider.getNoop();

    public static void configure(InstallationContext context) {
        WebsocketEventGenerator.logger =
                context.getOpenTelemetry().getLogsBridge().loggerBuilder(SCOPE).build();
        WebsocketEventGenerator.sessionProvider = context.getSessionProvider();
    }

    @Incubating
    public static void generateEvent(String eventName, Attributes attributes) {
        LogRecordBuilder logRecordBuilder = logger.logRecordBuilder();
        SessionExtensionsKt.setSessionIdentifiersWith(logRecordBuilder, sessionProvider);
        logRecordBuilder.setEventName(eventName).setAllAttributes(attributes).emit();
    }
}
