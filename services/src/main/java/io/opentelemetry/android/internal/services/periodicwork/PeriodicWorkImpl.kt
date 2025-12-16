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
 *
 * <p>The loop interval determines how frequently this service checks its work queue for pending
 * tasks. For optimal performance with exporters, the loop interval should typically match or be
 * slightly shorter than the export frequency configured in the exporter (e.g.,
 * exportScheduleDelayMillis). If the loop interval is significantly longer than the export
 * frequency, the actual export timing may be delayed.
 */
internal class PeriodicWorkImpl(
    private val loopIntervalMillis: Long = PeriodicWork.DEFAULT_LOOP_INTERVAL_MS,
) : PeriodicWork {
    private val delegator = WorkerDelegator(loopIntervalMillis)

    init {
        delegator.run()
    }

    override fun enqueue(runnable: Runnable) {
        delegator.enqueue(runnable)
    }

    override fun close() {
        delegator.close()
    }

    companion object {
        // The minimum loop interval is 1 second to allow for flexible scheduling
        internal const val MINIMUM_LOOP_INTERVAL_MILLIS: Long = 1000L
    }

    private class WorkerDelegator(
        private val loopIntervalMillis: Long,
    ) : Runnable,
        Closeable {
        companion object {
            private const val SECONDS_TO_KILL_IDLE_THREADS = 30L
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
            handler.postDelayed(this, loopIntervalMillis)
        }
    }
}
