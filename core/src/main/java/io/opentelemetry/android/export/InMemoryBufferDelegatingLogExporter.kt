/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An in-memory buffer delegating log exporter that buffers log records in memory until a delegate is set.
 * Once a delegate is set, the buffered log records are exported to the delegate.
 *
 * The buffer size is set to 5,000 by default. If the buffer is full, the exporter will drop new log records.
 */
class InMemoryBufferDelegatingLogExporter(
    private val bufferSize: Int = 5_000,
) : LogRecordExporter {
    private var delegate: LogRecordExporter? = null
    private val buffer = ArrayBlockingQueue<LogRecordData>(bufferSize)
    private val lock = ReentrantLock()

    /**
     * Sets the delegate and flushes the buffer to the delegate.
     */
    fun setDelegateAndFlush(logRecordExporter: LogRecordExporter) {
        lock.withLock {
            if (delegate != null) return
            delegate = logRecordExporter
            flushToDelegate()
        }
    }

    override fun close() {
        shutdown()
    }

    override fun export(logs: Collection<LogRecordData>): CompletableResultCode =
        lock.withLock {
            delegate?.export(logs) ?: run {
                logs.forEach(buffer::offer)
                CompletableResultCode.ofSuccess()
            }
        }

    override fun flush(): CompletableResultCode {
        flushToDelegate()
        return delegate?.flush() ?: CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode {
        flushToDelegate()
        return delegate?.shutdown() ?: CompletableResultCode.ofSuccess()
    }

    private fun flushToDelegate() {
        delegate?.export(buffer)
        buffer.clear()
    }
}
