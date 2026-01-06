/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import io.opentelemetry.android.common.internal.utils.threadIdCompat
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_ID
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME
import java.util.concurrent.TimeUnit

internal class CrashReporter(
    additionalExtractors: List<EventAttributesExtractor<CrashDetails>>,
    private val mode: CrashReportingMode = CrashReportingMode.LOGS_ONLY,
) {
    private val extractors: List<EventAttributesExtractor<CrashDetails>> =
        additionalExtractors.toList()

    /** Installs the crash reporting instrumentation.  */
    fun install(openTelemetry: OpenTelemetrySdk) {
        val handler =
            CrashReportingExceptionHandler(
                crashProcessor = { crashDetails: CrashDetails ->
                    processCrash(openTelemetry, crashDetails)
                },
                postCrashAction = {
                    waitForCrashFlush(openTelemetry)
                },
            )
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    private fun processCrash(
        openTelemetry: OpenTelemetrySdk,
        crashDetails: CrashDetails,
    ) {
        val attributes = buildCrashAttributes(crashDetails)

        // Emit log if mode includes logs
        if (mode != CrashReportingMode.SPANS_ONLY) {
            emitCrashLog(openTelemetry, attributes)
        }

        // Emit span if mode includes spans
        if (mode != CrashReportingMode.LOGS_ONLY) {
            emitCrashSpan(openTelemetry, crashDetails, attributes)
        }
    }

    private fun buildCrashAttributes(crashDetails: CrashDetails): Attributes {
        val throwable = crashDetails.cause
        val thread = crashDetails.thread
        val attributesBuilder =
            Attributes
                .builder()
                .put(THREAD_ID, thread.threadIdCompat)
                .put(THREAD_NAME, thread.name)
                .put(EXCEPTION_MESSAGE, throwable.message)
                .put(EXCEPTION_STACKTRACE, throwable.stackTraceToString())
                .put(EXCEPTION_TYPE, throwable.javaClass.name)

        for (extractor in extractors) {
            val extractedAttributes = extractor.extract(Context.current(), crashDetails)
            attributesBuilder.putAll(extractedAttributes)
        }
        return attributesBuilder.build()
    }

    private fun emitCrashLog(
        openTelemetry: OpenTelemetrySdk,
        attributes: Attributes,
    ) {
        val logger =
            openTelemetry.sdkLoggerProvider
                .loggerBuilder("io.opentelemetry.crash")
                .build()
        logger
            .logRecordBuilder()
            .setEventName("device.crash")
            .setAllAttributes(attributes)
            .emit()
    }

    private fun emitCrashSpan(
        openTelemetry: OpenTelemetrySdk,
        crashDetails: CrashDetails,
        attributes: Attributes,
    ) {
        val tracer =
            openTelemetry.sdkTracerProvider
                .tracerBuilder("io.opentelemetry.crash")
                .build()

        val span =
            tracer
                .spanBuilder("device.crash")
                .setAllAttributes(attributes)
                .startSpan()

        // Record exception as span event (OTel semconv for errors)
        span.recordException(crashDetails.cause)
        span.setStatus(StatusCode.ERROR, crashDetails.cause.message ?: "Application crash")
        span.end()
    }

    private fun waitForCrashFlush(openTelemetry: OpenTelemetrySdk) {
        // Flush logs if we emitted them
        if (mode != CrashReportingMode.SPANS_ONLY) {
            openTelemetry.sdkLoggerProvider.forceFlush().join(5, TimeUnit.SECONDS)
        }
        // Flush traces if we emitted them
        if (mode != CrashReportingMode.LOGS_ONLY) {
            openTelemetry.sdkTracerProvider.forceFlush().join(5, TimeUnit.SECONDS)
        }
    }
}
