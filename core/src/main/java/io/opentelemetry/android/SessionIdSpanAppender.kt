/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.context.Context
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.SessionAttributes.SESSION_ID
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

/**
 * A [SpanProcessor] that sets the `session.id` attribute to the current span when the span is started.
 */
@OptIn(IncubatingApi::class)
internal class SessionIdSpanAppender(
    private val sessionProvider: SessionProvider,
) : SpanProcessor {
    private val sessionId = stringKey(SESSION_ID)

    @OptIn(IncubatingApi::class)
    override fun onStart(
        parentContext: Context,
        span: ReadWriteSpan,
    ) {
        span.setAttribute(sessionId, sessionProvider.getSessionId())
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {}

    override fun isEndRequired(): Boolean = false
}
