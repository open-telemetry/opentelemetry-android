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
import java.net.IDN

/**
 * Drops HTTP client spans whose host is not on an allowlist.
 *
 * Holds its own copy of the host names, so it is safe to read from the exporter thread
 * while the configuration object it was built from is still reachable.
 */
internal class HttpSpanHostFilter private constructor(
    private val allowedHosts: Set<String>,
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
        // A host with no punycode form cannot be one that was configured, since
        // configuration accepts ASCII only, so it is not on the allowlist.
        val candidate = punycode(host.lowercase()) ?: return true
        return candidate !in allowedHosts
    }

    companion object {
        private const val MAX_ASCII = 127

        /** Set by the HTTP client instrumentations. */
        private val SERVER_ADDRESS = AttributeKey.stringKey(ServerAttributes.SERVER_ADDRESS)

        /** Required on every HTTP span, so its absence means the span is not HTTP. */
        private val HTTP_REQUEST_METHOD = AttributeKey.stringKey(HttpAttributes.HTTP_REQUEST_METHOD)

        /** Returns null when no host was configured, so that no filtering is installed. */
        fun create(allowedHosts: Set<String>): HttpSpanHostFilter? = if (allowedHosts.isEmpty()) null else HttpSpanHostFilter(allowedHosts)

        /**
         * Converts a recorded host to punycode, because `HttpURLConnection` reports whatever
         * host the caller wrote while configured hosts are always ASCII.
         *
         * A host that is already ASCII is returned unchanged rather than round-tripped, so
         * this normalizes spelling without imposing IDN label rules on it. Returns null for a
         * host with no punycode form.
         */
        private fun punycode(host: String): String? =
            if (host.all { it.code <= MAX_ASCII }) {
                host
            } else {
                try {
                    IDN.toASCII(host).lowercase()
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
    }
}
