/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_ESCAPED;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.*;

import io.opentelemetry.android.instrumentation.common.InstrumentedApplication;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;

/** Entrypoint for installing the crash reporting instrumentation. */
public final class CrashReporter {

    /** Returns a new {@link CrashReporter} with the default settings. */
    public static CrashReporter create() {
        return builder().build();
    }

    /** Returns a new {@link CrashReporterBuilder}. */
    public static CrashReporterBuilder builder() {
        return new CrashReporterBuilder();
    }

    private final List<AttributesExtractor<CrashDetails, Void>> additionalExtractors;

    CrashReporter(CrashReporterBuilder builder) {
        this.additionalExtractors = builder.additionalExtractors;
    }

    /**
     * Installs the crash reporting instrumentation on the given {@link InstrumentedApplication}.
     */
    public void installOn(InstrumentedApplication instrumentedApplication) {
        Thread.UncaughtExceptionHandler existingHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(
                new CrashReportingExceptionHandler(
                        buildInstrumenter(
                                instrumentedApplication
                                        .getOpenTelemetrySdk()
                                        .getSdkLoggerProvider()),
                        instrumentedApplication.getOpenTelemetrySdk().getSdkLoggerProvider(),
                        existingHandler));
    }

    private void emitCrashEvent(Logger crashReporter, CrashDetails crashDetails) {
        Throwable throwable = crashDetails.getCause();
        Thread thread = crashDetails.getThread();
        AttributesBuilder attributesBuilder =
                Attributes.builder()
                        .put(EXCEPTION_ESCAPED, true)
                        .put(THREAD_ID, thread.getId())
                        .put(THREAD_NAME, thread.getName())
                        .put(EXCEPTION_MESSAGE, throwable.getMessage())
                        .put(EXCEPTION_STACKTRACE, stackTraceToString(throwable))
                        .put(EXCEPTION_TYPE, throwable.getClass().getName());

        for (AttributesExtractor<CrashDetails, Void> extractor : additionalExtractors) {
            extractor.onStart(attributesBuilder, Context.current(), crashDetails);
        }

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
