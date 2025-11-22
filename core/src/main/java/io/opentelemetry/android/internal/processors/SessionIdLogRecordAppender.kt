/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.opentelemetry.android.ktx.setSessionIdentifiersWith
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

/**
 * A [LogRecordProcessor] that sets the session identifiers to log records when they are emitted.
 *
 * This processor adds both the current session ID and the previous session ID (if available)
 * to enable session transition tracking and correlation across session boundaries.
 */
internal class SessionIdLogRecordAppender(
    private val sessionProvider: SessionProvider,
) : LogRecordProcessor {
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        logRecord.setSessionIdentifiersWith(sessionProvider)
    }
}
