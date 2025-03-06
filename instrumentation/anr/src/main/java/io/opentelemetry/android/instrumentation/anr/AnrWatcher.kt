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
import java.util.concurrent.atomic.AtomicInteger

internal class AnrWatcher(
    private val uiHandler: Handler,
    private val mainThread: Thread,
    private val instrumenter: Instrumenter<Array<StackTraceElement>, Void>
) : Runnable {
    private val anrCounter = AtomicInteger()

    override fun run() {
        val response = CountDownLatch(1)
        if (!uiHandler.post { response.countDown() }) {
            // the main thread is probably shutting down. ignore and return.
            return
        }
        val success: Boolean
        try {
            success = response.await(1, TimeUnit.SECONDS)
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
