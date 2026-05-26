/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.context.Context
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.kotlin.semconv.SessionAttributes.SESSION_ID

internal class SessionIdLogRecordAppender(
    private val sessionProvider: SessionProvider,
) : LogRecordProcessor {
    @OptIn(IncubatingApi::class)
    val sessionIdKey: AttributeKey<String?> = stringKey(SESSION_ID)
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        logRecord.setAttribute(sessionIdKey, sessionProvider.getSessionId())
    }
}
