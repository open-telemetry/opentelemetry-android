/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

/**
 * An in-memory buffer delegating span exporter that buffers span data in memory until a delegate is set.
 * Once a delegate is set, the buffered span data is exported to the delegate.
 *
 * The buffer size is set to 5,000 spans by default. If the buffer is full, the exporter will drop new span data.
 */
internal class BufferDelegatingSpanExporter(
    maxBufferedSpans: Int = 5_000,
) : SpanExporter {
    private val delegatingExporter =
        DelegatingExporter<SpanExporter, SpanData>(
            doExport = SpanExporter::export,
            doFlush = SpanExporter::flush,
            doShutdown = SpanExporter::shutdown,
            maxBufferedData = maxBufferedSpans,
            logType = "span data",
        )

    fun setDelegate(delegate: SpanExporter) {
        delegatingExporter.setDelegate(delegate)
    }

    override fun export(spans: Collection<SpanData>): CompletableResultCode = delegatingExporter.export(spans)

    override fun flush(): CompletableResultCode = delegatingExporter.flush()

    override fun shutdown(): CompletableResultCode = delegatingExporter.shutdown()
}
