/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import java.util.function.Supplier

internal class GlobalAttributesLogRecordAppender(
    private val attributesSupplier: Supplier<Attributes>,
) : LogRecordProcessor {
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        logRecord.setAllAttributes(attributesSupplier.get())
    }
}
