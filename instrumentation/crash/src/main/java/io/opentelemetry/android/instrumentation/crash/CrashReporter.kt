/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import io.opentelemetry.android.common.internal.SemconvCompat.Companion.map
import io.opentelemetry.android.common.internal.utils.threadIdCompat
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.kotlin.semconv.ExceptionAttributes.EXCEPTION_MESSAGE
import io.opentelemetry.kotlin.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE
import io.opentelemetry.kotlin.semconv.ExceptionAttributes.EXCEPTION_TYPE
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.ThreadAttributes.THREAD_ID
import io.opentelemetry.kotlin.semconv.ThreadAttributes.THREAD_NAME
import java.io.PrintWriter
import java.io.StringWriter

internal class CrashReporter(
    additionalExtractors: List<EventAttributesExtractor<CrashDetails>>,
) {
    private val extractors: List<EventAttributesExtractor<CrashDetails>> =
        additionalExtractors.toList()

    /**
     * Silently ignores printing after a `threshold`
     */
    private class MaxLinesPrintWriter(
        sw: StringWriter,
        private val threshold: Int = 1_000,
    ) : PrintWriter(sw) {
        var stringCount = 0
        var lineCount = 0

        override fun print(s: String?) {
            if (stringCount < threshold) {
                super.print(s)
                stringCount++
            }
        }

        override fun println() {
            if (lineCount < threshold) {
                super.println()
                lineCount++
            }
        }
    }

    /** Installs the crash reporting instrumentation.  */
    fun install(openTelemetry: OpenTelemetry) {
        val handler =
            CrashReportingExceptionHandler(
                crashProcessor = { crashDetails: CrashDetails ->
                    processCrash(openTelemetry, crashDetails)
                },
            )
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    private fun stackTraceToTruncatedString(
        throwable: Throwable,
        truncateThreshold: Int = 1000,
    ): String {
        val stringWriter = StringWriter()
        val printWriter = MaxLinesPrintWriter(stringWriter, truncateThreshold)
        throwable.printStackTrace(printWriter)
        printWriter.flush()
        return stringWriter.toString()
    }

    @OptIn(IncubatingApi::class)
    private fun processCrash(
        openTelemetry: OpenTelemetry,
        crashDetails: CrashDetails,
    ) {
        val logger = openTelemetry.logsBridge.loggerBuilder("io.opentelemetry.crash").build()
        val throwable = crashDetails.cause
        val thread = crashDetails.thread
        val attributesBuilder =
            Attributes
                .builder()
                .put(THREAD_ID, thread.threadIdCompat)
                .put(THREAD_NAME, thread.name)
                .put(EXCEPTION_MESSAGE, throwable.message)
                .put(
                    EXCEPTION_STACKTRACE,
                    stackTraceToTruncatedString(throwable),
                ).put(EXCEPTION_TYPE, throwable.javaClass.name)
        for (extractor in extractors) {
            val extractedAttributes = extractor.extract(Context.current(), crashDetails)
            attributesBuilder.putAll(extractedAttributes)
        }
        val eventBuilder =
            logger.logRecordBuilder()
        eventBuilder
            .setEventName(map("app.crash"))
            .setAllAttributes(attributesBuilder.build())
            .emit()
    }
}
