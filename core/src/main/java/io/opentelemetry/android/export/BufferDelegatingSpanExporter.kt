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
 * The buffer size is set to 5,000 by default. If the buffer is full, the exporter will drop new span data.
 */
internal class BufferDelegatingSpanExporter(
    bufferSize: Int = 5_000
): BufferedDelegatingExporter<SpanData, SpanExporter>(bufferSize = bufferSize), SpanExporter {

    override fun exportToDelegate(delegate: SpanExporter, data: Collection<SpanData>): CompletableResultCode {
        return delegate.export(data)
    }

    override fun shutdownDelegate(delegate: SpanExporter): CompletableResultCode {
        return delegate.shutdown()
    }

    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        return bufferOrDelegate(spans)
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

