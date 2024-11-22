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
) : BufferedDelegatingExporter<SpanData, SpanExporter>(bufferedSignals = maxBufferedSpans),
    SpanExporter {
    override fun exportToDelegate(
        delegate: SpanExporter,
        data: Collection<SpanData>,
    ): CompletableResultCode = delegate.export(data)

    override fun shutdownDelegate(delegate: SpanExporter): CompletableResultCode = delegate.shutdown()

    override fun export(spans: Collection<SpanData>): CompletableResultCode = bufferOrDelegate(spans)

    override fun flush(): CompletableResultCode =
        withDelegateOrNull { delegate ->
            delegate?.flush() ?: CompletableResultCode.ofSuccess()
        }
}
