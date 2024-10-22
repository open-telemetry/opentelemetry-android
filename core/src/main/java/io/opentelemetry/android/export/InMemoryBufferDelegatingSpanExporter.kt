/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An in-memory buffer delegating span exporter that buffers span data in memory until a delegate is set.
 * Once a delegate is set, the buffered span data is exported to the delegate.
 *
 * The buffer size is set to 5,000 by default. If the buffer is full, the exporter will drop new span data.
 */
class InMemoryBufferDelegatingSpanExporter(
    private val bufferSize: Int = 5_000,
) : SpanExporter {
    private var delegate: SpanExporter? = null
    private val buffer = ArrayBlockingQueue<SpanData>(bufferSize)
    private val lock = ReentrantLock()

    /**
     * Sets the delegate and flushes the buffer to the delegate.
     */
    fun setDelegateAndFlush(spanExporter: SpanExporter) {
        lock.withLock {
            if (delegate != null) return
            delegate = spanExporter
            flushToDelegate()
        }
    }

    override fun close() {
        shutdown()
    }

    override fun export(spans: Collection<SpanData>): CompletableResultCode =
        lock.withLock {
            delegate?.export(spans) ?: run {
                spans.forEach(buffer::offer)
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
