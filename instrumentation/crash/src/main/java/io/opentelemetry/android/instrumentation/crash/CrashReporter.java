/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.incubating.EventIncubatingAttributes.EVENT_NAME;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_ID;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;

public final class CrashReporter {
    private final List<AttributesExtractor<CrashDetails, Void>> additionalExtractors;

    public CrashReporter(List<AttributesExtractor<CrashDetails, Void>> additionalExtractors) {
        this.additionalExtractors = additionalExtractors;
    }

    /** Installs the crash reporting instrumentation. */
    public void install(OpenTelemetrySdk openTelemetry) {
        Thread.UncaughtExceptionHandler existingHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(
                new CrashReportingExceptionHandler(
                        buildInstrumenter(openTelemetry.getSdkLoggerProvider()),
                        openTelemetry.getSdkLoggerProvider(), // TODO avoid using OpenTelemetrySdk
                        // methods, only use the ones from
                        // OpenTelemetry api.
                        existingHandler));
    }

    private void emitCrashEvent(Logger crashReporter, CrashDetails crashDetails) {
        Throwable throwable = crashDetails.getCause();
        Thread thread = crashDetails.getThread();
        AttributesBuilder attributesBuilder =
                Attributes.builder()
                        .put(THREAD_ID, thread.getId())
                        .put(THREAD_NAME, thread.getName())
                        .put(EXCEPTION_MESSAGE, throwable.getMessage())
                        .put(EXCEPTION_STACKTRACE, stackTraceToString(throwable))
                        .put(EXCEPTION_TYPE, throwable.getClass().getName());

        for (AttributesExtractor<CrashDetails, Void> extractor : additionalExtractors) {
            extractor.onStart(attributesBuilder, Context.current(), crashDetails);
        }

        // TODO: use emitEvent() when available, with event name from semantic conventions.
        attributesBuilder.put(EVENT_NAME, "device.crash");
        crashReporter.logRecordBuilder().setAllAttributes(attributesBuilder.build()).emit();
    }

    private String stackTraceToString(Throwable throwable) {
        StringWriter sw = new StringWriter(256);
        PrintWriter pw = new PrintWriter(sw);

        throwable.printStackTrace(pw);
        pw.flush();

        return sw.toString();
    }

    private Consumer<CrashDetails> buildInstrumenter(LoggerProvider loggerProvider) {
        Logger logger = loggerProvider.loggerBuilder("io.opentelemetry.crash").build();
        return crashDetails -> emitCrashEvent(logger, crashDetails);
    }
}
