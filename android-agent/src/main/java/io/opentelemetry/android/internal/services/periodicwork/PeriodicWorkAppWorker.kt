/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodicwork

import android.os.Handler
import android.os.Looper
import io.opentelemetry.android.internal.services.AppWorker
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Utility to run periodic background work.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
class PeriodicWorkAppWorker : AppWorker {
    private val delegator = WorkerDelegator()
    private val started = AtomicBoolean(false)

    override fun start() {
        if (!started.getAndSet(true)) {
            delegator.run()
        }
    }

    fun enqueue(runnable: Runnable) {
        delegator.enqueue(runnable)
    }

    private class WorkerDelegator : Runnable {
        companion object {
            private const val SECONDS_TO_KILL_IDLE_THREADS = 30L
            private const val SECONDS_FOR_NEXT_LOOP = 10L
            private const val MAX_AMOUNT_OF_WORKER_THREADS = 1
            private const val NUMBER_OF_PERMANENT_WORKER_THREADS = 0
        }

        private val queue = ConcurrentLinkedQueue<Runnable>()
        private val handler = Handler(Looper.getMainLooper())
        private val executor =
            ThreadPoolExecutor(
                NUMBER_OF_PERMANENT_WORKER_THREADS,
                MAX_AMOUNT_OF_WORKER_THREADS,
                SECONDS_TO_KILL_IDLE_THREADS,
                TimeUnit.SECONDS,
                LinkedBlockingQueue(),
            )

        fun enqueue(runnable: Runnable) {
            queue.add(runnable)
        }

        override fun run() {
            delegateToWorkerThread()
            scheduleNextLookUp()
        }

        private fun delegateToWorkerThread() {
            while (queue.isNotEmpty()) {
                executor.execute(queue.poll())
            }
        }

        private fun scheduleNextLookUp() {
            handler.postDelayed(this, TimeUnit.SECONDS.toMillis(SECONDS_FOR_NEXT_LOOP))
        }
    }
}
