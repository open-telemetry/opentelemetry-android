/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

/** Wrap the exporting processor, not a sibling processor, to filter before batching or buffering. */
internal class FilteringLogRecordProcessor(
    private val delegate: LogRecordProcessor,
    private val predicate: (ReadWriteLogRecord) -> Boolean,
) : LogRecordProcessor {
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        if (predicate(logRecord)) delegate.onEmit(context, logRecord)
    }

    // Kotlin delegation leaves these Java default methods unforwarded.
    override fun forceFlush(): CompletableResultCode = delegate.forceFlush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()

    override fun close() = delegate.close()
}
