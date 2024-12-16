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
 * The buffer size is set to 5,000 log entries by default. If the buffer is full, the exporter will drop new log records.
 */
internal class BufferDelegatingLogExporter(
    maxBufferedLogs: Int = 5_000,
) : LogRecordExporter {
    private val delegatingExporter =
        DelegatingExporter<LogRecordExporter, LogRecordData>(
            doExport = LogRecordExporter::export,
            doFlush = LogRecordExporter::flush,
            doShutdown = LogRecordExporter::shutdown,
            maxBufferedData = maxBufferedLogs,
            logType = "log records",
        )

    fun setDelegate(delegate: LogRecordExporter) {
        delegatingExporter.setDelegate(delegate)
    }

    override fun export(logs: Collection<LogRecordData>): CompletableResultCode = delegatingExporter.export(logs)

    override fun flush(): CompletableResultCode = delegatingExporter.flush()

    override fun shutdown(): CompletableResultCode = delegatingExporter.shutdown()
}
