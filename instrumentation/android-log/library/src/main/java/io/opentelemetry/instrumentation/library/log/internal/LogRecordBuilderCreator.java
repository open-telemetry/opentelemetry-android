/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.log.internal;

import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder;
import io.opentelemetry.api.incubator.logs.ExtendedLogger;
import java.io.PrintWriter;
import java.io.StringWriter;

public class LogRecordBuilderCreator {

    private LogRecordBuilderCreator() {}

    private static ExtendedLogger logger =
            (ExtendedLogger)
                    OpenTelemetry.noop()
                            .getLogsBridge()
                            .loggerBuilder("io.opentelemetry.android.log.noop")
                            .build();

    public static void configure(InstallationContext context) {
        logger =
                (ExtendedLogger)
                        context.getOpenTelemetry()
                                .getLogsBridge()
                                .loggerBuilder("io.opentelemetry.android.log")
                                .build();
    }

    public static ExtendedLogRecordBuilder createLogRecordBuilder() {
        return logger.logRecordBuilder();
    }

    public static String printStacktrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
        }
        return stringWriter.toString();
    }

    public static String getEventName(Throwable throwable) {
        String eventName = throwable.getClass().getCanonicalName();
        if (eventName == null) eventName = throwable.getClass().getSimpleName();
        return eventName;
    }
}
