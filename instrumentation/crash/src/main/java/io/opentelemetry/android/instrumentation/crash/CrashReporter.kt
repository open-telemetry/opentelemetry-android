/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.android.common.internal.utils.threadIdCompat
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.ktx.setSessionIdentifiersWith
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_ID
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME
import java.util.concurrent.TimeUnit

@OptIn(Incubating::class)
internal class CrashReporter(
    private val sessionProvider: SessionProvider,
    additionalExtractors: List<EventAttributesExtractor<CrashDetails>>,
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
        val logger = openTelemetry.sdkLoggerProvider.loggerBuilder("io.opentelemetry.crash").build()
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
                    throwable.stackTraceToString(),
                ).put(EXCEPTION_TYPE, throwable.javaClass.name)
        for (extractor in extractors) {
            val extractedAttributes = extractor.extract(Context.current(), crashDetails)
            attributesBuilder.putAll(extractedAttributes)
        }
        logger
            .logRecordBuilder()
            .setSessionIdentifiersWith(sessionProvider)
            .setEventName("device.crash")
            .setAllAttributes(attributesBuilder.build())
            .emit()
    }

    private fun waitForCrashFlush(openTelemetry: OpenTelemetrySdk) {
        val flushResult = openTelemetry.sdkLoggerProvider.forceFlush()
        flushResult.join(10, TimeUnit.SECONDS)
    }
}
