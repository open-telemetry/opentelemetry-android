/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import java.util.function.Predicate

/** Wrap the exporting processor, not a sibling processor, to filter before batching or buffering. */
internal class FilteringLogRecordProcessor(
    private val delegate: LogRecordProcessor,
    private val predicate: Predicate<ReadWriteLogRecord>,
) : LogRecordProcessor {
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        if (predicate.test(logRecord)) delegate.onEmit(context, logRecord)
    }

    override fun forceFlush(): CompletableResultCode = delegate.forceFlush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()

    override fun close() = delegate.close()
}
