/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import io.embrace.opentelemetry.kotlin.ExperimentalApi
import io.embrace.opentelemetry.kotlin.attributes.setAttributes
import io.embrace.opentelemetry.kotlin.getLogger
import io.embrace.opentelemetry.kotlin.toOtelKotlinApi
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
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

    @OptIn(ExperimentalApi::class)
    private fun processCrash(
        openTelemetry: OpenTelemetrySdk,
        crashDetails: CrashDetails,
    ) {
        val otel = openTelemetry.toOtelKotlinApi()
        val logger = otel.getLogger("io.opentelemetry.crash")
        val throwable = crashDetails.cause
        val thread = crashDetails.thread

        val attributesBuilder = Attributes.builder()
        for (extractor in extractors) {
            val extractedAttributes = extractor.extract(Context.current(), crashDetails)
            attributesBuilder.putAll(extractedAttributes)
        }

        logger.log(body = "device.crash") {
            // event name not supported yet. Use body as an example instead.
            setAttributes(
                mapOf<String, Any>(
                    THREAD_ID.key to thread.id,
                    THREAD_NAME.key to thread.name,
                    EXCEPTION_STACKTRACE.key to throwable.stackTraceToString(),
                    EXCEPTION_TYPE.key to throwable.javaClass.name,
                ),
            )
            setAttributes(attributesBuilder.toMap())

            throwable.message?.let {
                setStringAttribute(EXCEPTION_MESSAGE.key, it)
            }
        }
    }

    private fun AttributesBuilder.toMap(): Map<String, Any> = build().asMap().mapKeys { (k, _) -> k.key }

    private fun waitForCrashFlush(openTelemetry: OpenTelemetrySdk) {
        val flushResult = openTelemetry.sdkLoggerProvider.forceFlush()
        flushResult.join(10, TimeUnit.SECONDS)
    }
}
