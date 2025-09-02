/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodicwork

import android.os.Handler
import android.os.Looper
import java.io.Closeable
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
internal class PeriodicWorkImpl : PeriodicWork {
    private val delegator = WorkerDelegator()

    init {
        delegator.run()
    }

    override fun enqueue(runnable: Runnable) {
        delegator.enqueue(runnable)
    }

    override fun close() {
        delegator.close()
    }

    private class WorkerDelegator :
        Runnable,
        Closeable {
        companion object {
            private const val SECONDS_TO_KILL_IDLE_THREADS = 30L
            private const val SECONDS_FOR_NEXT_LOOP = 10L
            private const val MAX_AMOUNT_OF_WORKER_THREADS = 1
            private const val NUMBER_OF_PERMANENT_WORKER_THREADS = 0
        }

        private val closed = AtomicBoolean(false)
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
            if (closed.get()) {
                return
            }
            queue.add(runnable)
        }

        override fun run() {
            if (closed.get()) {
                return
            }

            delegateToWorkerThread()
            scheduleNextLookUp()
        }

        override fun close() {
            if (!closed.compareAndSet(false, true)) {
                queue.clear()
            }
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
