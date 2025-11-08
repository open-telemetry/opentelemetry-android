/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.ktx.setSessionIdentifiersWith
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

/**
 * A [SpanProcessor] that sets the session identifiers to the current span when the span is started.
 *
 * This processor adds both the current session ID and the previous session ID (if available)
 * to enable session transition tracking and correlation across session boundaries.
 */
internal class SessionIdSpanAppender(
    private val sessionProvider: SessionProvider,
) : SpanProcessor {
    override fun onStart(
        parentContext: Context,
        span: ReadWriteSpan,
    ) {
        span.setSessionIdentifiersWith(sessionProvider)
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {}

    override fun isEndRequired(): Boolean = false
}
