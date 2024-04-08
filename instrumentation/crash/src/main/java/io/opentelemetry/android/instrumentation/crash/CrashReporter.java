/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.SemanticAttributes;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;

/** Entrypoint for installing the crash reporting instrumentation. */
public final class CrashReporter {
    private final List<AttributesExtractor<CrashDetails, Void>> additionalExtractors;

    public CrashReporter(List<AttributesExtractor<CrashDetails, Void>> additionalExtractors) {
        this.additionalExtractors = additionalExtractors;
    }

    /**
     * Installs the crash reporting instrumentation on the given {@link InstrumentedApplication}.
     */
    public void install(OpenTelemetryRum openTelemetryRum) {
        Thread.UncaughtExceptionHandler existingHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(
                new CrashReportingExceptionHandler(
                        buildInstrumenter(openTelemetryRum.getOpenTelemetry().getLogsBridge()),
                        openTelemetryRum
                                .getOpenTelemetry()
                                .getSdkLoggerProvider(), // TODO avoid using OpenTelemetrySdk
                        // methods, only use the ones from
                        // OpenTelemetry.
                        existingHandler));
    }

    private void emitCrashEvent(Logger crashReporter, CrashDetails crashDetails) {
        Throwable throwable = crashDetails.getCause();
        Thread thread = crashDetails.getThread();
        AttributesBuilder attributesBuilder =
                Attributes.builder()
                        .put(SemanticAttributes.EXCEPTION_ESCAPED, true)
                        .put(SemanticAttributes.THREAD_ID, thread.getId())
                        .put(SemanticAttributes.THREAD_NAME, thread.getName())
                        .put(SemanticAttributes.EXCEPTION_MESSAGE, throwable.getMessage())
                        .put(SemanticAttributes.EXCEPTION_STACKTRACE, stackTraceToString(throwable))
                        .put(SemanticAttributes.EXCEPTION_TYPE, throwable.getClass().getName());

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
