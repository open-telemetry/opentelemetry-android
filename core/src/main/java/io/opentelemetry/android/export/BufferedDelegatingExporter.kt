/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.sdk.common.CompletableResultCode
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An in-memory buffer delegating signal exporter that buffers signal in memory until a delegate is set.
 * Once a delegate is set, the buffered signals are exported to the delegate.
 *
 * The buffer size is set to 5,000 by default. If the buffer is full, the exporter will drop new signals.
 */
internal abstract class BufferedDelegatingExporter<T, D>(private val bufferedSignals: Int = 5_000) {
    @Volatile
    private var delegate: D? = null
    private val buffer = arrayListOf<T>()
    private val lock = Any()
    private var isShutDown = AtomicBoolean(false)

    /**
     * Sets the delegate for this exporter and flushes the buffer to the delegate.
     *
     * If the delegate has already been set, an [IllegalStateException] will be thrown.
     * If this exporter has been shut down, the delegate will be shut down immediately.
     *
     * @param delegate the delegate to set
     *
     *  @throws IllegalStateException if a delegate has already been set
     */
    fun setDelegate(delegate: D) {
        synchronized(lock) {
            check(this.delegate == null) { "Exporter delegate has already been set." }

            flushToDelegate(delegate)

            this.delegate = delegate

            if (isShutDown.get()) {
                shutdownDelegate(delegate)
            }
        }
    }

    /**
     * Buffers the given data if the delegate has not been set, otherwise exports the data to the delegate.
     *
     * @param data the data to buffer or export
     */
    protected fun bufferOrDelegate(data: Collection<T>): CompletableResultCode =
        withDelegateOrNull {
            if (it != null) {
                exportToDelegate(it, data)
            } else {
                val amountToTake = bufferedSignals - buffer.size
                buffer.addAll(data.take(amountToTake))
                CompletableResultCode.ofSuccess()
            }
        }

    /**
     * Executes the given block with the delegate if it has been set, otherwise executes the block with a null delegate.
     *
     * @param block the block to execute
     */
    protected fun <R> withDelegateOrNull(block: (D?) -> R): R {
        delegate?.let { return block(it) }
        return synchronized(lock) { block(delegate) }
    }

    open fun shutdown(): CompletableResultCode = bufferedShutDown()

    protected abstract fun exportToDelegate(
        delegate: D,
        data: Collection<T>,
    ): CompletableResultCode

    protected abstract fun shutdownDelegate(delegate: D): CompletableResultCode

    private fun flushToDelegate(delegate: D) {
        exportToDelegate(delegate, buffer)
        buffer.clear()
        buffer.trimToSize()
    }

    private fun bufferedShutDown(): CompletableResultCode {
        isShutDown.set(true)

        return withDelegateOrNull {
            if (it != null) {
                shutdownDelegate(it)
            } else {
                CompletableResultCode.ofSuccess()
            }
        }
    }
}
