/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import android.os.Handler
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
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
internal class AnrWatcher(
    private val uiHandler: Handler,
    private val mainThread: Thread,
    private val instrumenter: Instrumenter<Array<StackTraceElement>, Void>,
    private val pollDurationNs: Long = DEFAULT_POLL_DURATION_NS,
) : Runnable {
    private val anrCounter = AtomicInteger()

    constructor(uiHandler: Handler, mainThread: Thread, instrumenter: Instrumenter<Array<StackTraceElement>, Void>) :
        this(uiHandler, mainThread, instrumenter, DEFAULT_POLL_DURATION_NS)

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
            recordAnr(stackTrace)
            // only report once per 5s.
            anrCounter.set(0)
        }
    }

    private fun recordAnr(stackTrace: Array<StackTraceElement>) {
        val context = instrumenter.start(Context.current(), stackTrace)
        instrumenter.end(context, stackTrace, null, null)
    }
}
