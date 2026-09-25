/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.kotlin.semconv.HttpAttributes
import io.opentelemetry.kotlin.semconv.ServerAttributes
import io.opentelemetry.sdk.trace.data.SpanData
import java.util.function.Predicate

/**
 * Drops HTTP client spans whose host is rejected by a predicate.
 */
internal class HttpSpanHostFilter private constructor(
    private val recordSpanForHost: Predicate<String>,
) {
    /**
     * Only HTTP client spans are considered. gRPC and database spans also record
     * `server.address`, and a span that records no host at all is always kept.
     */
    fun rejects(span: SpanData): Boolean {
        if (span.kind != SpanKind.CLIENT) {
            return false
        }
        val attributes = span.attributes
        if (attributes.get(HTTP_REQUEST_METHOD) == null) {
            return false
        }
        val host = attributes.get(SERVER_ADDRESS) ?: return false
        return !recordSpanForHost.test(host.lowercase())
    }

    companion object {
        /** Set by the HTTP client instrumentations. */
        private val SERVER_ADDRESS = AttributeKey.stringKey(ServerAttributes.SERVER_ADDRESS)

        /** Required on every HTTP span, so its absence means the span is not HTTP. */
        private val HTTP_REQUEST_METHOD = AttributeKey.stringKey(HttpAttributes.HTTP_REQUEST_METHOD)

        /** Returns null when no predicate was configured, so that no filtering is installed. */
        fun create(recordSpanForHost: Predicate<String>?): HttpSpanHostFilter? = recordSpanForHost?.let { HttpSpanHostFilter(it) }
    }
}
