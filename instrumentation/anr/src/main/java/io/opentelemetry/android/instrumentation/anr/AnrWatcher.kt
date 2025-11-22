/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import android.os.Handler
import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.android.common.internal.utils.threadIdCompat
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.ktx.setSessionIdentifiersWith
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.context.Context
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_ID
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

internal val DEFAULT_POLL_DURATION_NS = SECONDS.toNanos(1)

/**
 * Class that watches the ui thread for ANRs by posting
 * Runnables to the main thread. If 5 consecutive responses
 * time out, then an ANR is detected.
 *
 * @param pollDurationNs - exists for testing
 */
@OptIn(Incubating::class)
internal class AnrWatcher(
    private val uiHandler: Handler,
    private val mainThread: Thread,
    private val anrLogger: Logger,
    private val sessionProvider: SessionProvider,
    private val additionalExtractors: List<EventAttributesExtractor<Array<StackTraceElement>>>,
    private val pollDurationNs: Long = DEFAULT_POLL_DURATION_NS,
) : Runnable {
    private val anrCounter = AtomicInteger()

    constructor(uiHandler: Handler, mainThread: Thread, anrLogger: Logger, sessionProvider: SessionProvider) :
        this(uiHandler, mainThread, anrLogger, sessionProvider, emptyList(), DEFAULT_POLL_DURATION_NS)

    // A constructor that can be called from Java
    constructor(
        uiHandler: Handler,
        mainThread: Thread,
        anrLogger: Logger,
        sessionProvider: SessionProvider,
        additionalExtractors: List<EventAttributesExtractor<Array<StackTraceElement>>>,
    ) :
        this(uiHandler, mainThread, anrLogger, sessionProvider, additionalExtractors, DEFAULT_POLL_DURATION_NS)

    override fun run() {
        val response = CountDownLatch(1)
        if (!uiHandler.post { response.countDown() }) {
            // the main thread is probably shutting down. ignore and return.
            return
        }
        val success: Boolean
        try {
            success = response.await(pollDurationNs, TimeUnit.NANOSECONDS)
        } catch (e: InterruptedException) {
            return
        }
        if (success) {
            anrCounter.set(0)
            return
        }
        if (anrCounter.incrementAndGet() >= 5) {
            val stackTrace = mainThread.stackTrace
            emitAnrEvent(stackTrace)
            // only report once per 5s.
            anrCounter.set(0)
        }
    }

    private fun emitAnrEvent(stackTrace: Array<StackTraceElement>) {
        @Suppress("DEPRECATION")
        val id = mainThread.threadIdCompat
        val attributesBuilder =
            Attributes
                .builder()
                .put(THREAD_ID, id)
                .put(THREAD_NAME, mainThread.name)
                .put(EXCEPTION_STACKTRACE, stackTraceToString(stackTrace))

        for (extractor in additionalExtractors) {
            val extractedAttributes = extractor.extract(Context.current(), stackTrace)
            attributesBuilder.putAll(extractedAttributes)
        }

        anrLogger
            .logRecordBuilder()
            .setSessionIdentifiersWith(sessionProvider)
            .setEventName("device.anr")
            .setAllAttributes(attributesBuilder.build())
            .emit()
    }

    private fun stackTraceToString(stackTrace: Array<StackTraceElement>): String {
        val stackTraceString = StringBuilder()
        for (stackTraceElement in stackTrace) {
            stackTraceString.append(stackTraceElement).append("\n")
        }
        return stackTraceString.toString()
    }
}
