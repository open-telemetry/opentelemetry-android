/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_ID

/**
 * A [SpanProcessor] that sets the `session.id` attribute to the current span when the span is started.
 */
internal class SessionIdSpanAppender(
    private val sessionProvider: SessionProvider,
) : SpanProcessor {
    override fun onStart(
        parentContext: Context,
        span: ReadWriteSpan,
    ) {
        span.setAttribute(SESSION_ID, sessionProvider.getSessionId())
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {}

    override fun isEndRequired(): Boolean = false
}
