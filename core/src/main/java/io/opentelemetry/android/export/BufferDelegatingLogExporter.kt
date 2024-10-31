/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter

/**
 * An in-memory buffer delegating log exporter that buffers log records in memory until a delegate is set.
 * Once a delegate is set, the buffered log records are exported to the delegate.
 *
 * The buffer size is set to 5,000 by default. If the buffer is full, the exporter will drop new log records.
 */
internal class BufferDelegatingLogExporter(
    bufferSize: Int = 5_000
): BufferedDelegatingExporter<LogRecordData, LogRecordExporter>(bufferSize = bufferSize), LogRecordExporter {

    override fun exportToDelegate(delegate: LogRecordExporter, data: Collection<LogRecordData>): CompletableResultCode {
        return delegate.export(data)
    }

    override fun shutdownDelegate(delegate: LogRecordExporter): CompletableResultCode {
        return delegate.shutdown()
    }

    override fun export(logs: Collection<LogRecordData>): CompletableResultCode {
        return bufferOrDelegate(logs)
    }

    override fun flush(): CompletableResultCode {
        return withDelegateOrNull { delegate ->
            delegate?.flush() ?: CompletableResultCode.ofSuccess()
        }
    }

    override fun shutdown(): CompletableResultCode {
        return bufferedShutDown()
    }

    override fun close() {
        shutdown()
    }
}
