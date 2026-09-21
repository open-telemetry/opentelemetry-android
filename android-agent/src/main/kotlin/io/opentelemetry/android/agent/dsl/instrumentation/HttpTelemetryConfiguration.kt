/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.kotlin.semconv.HttpAttributes
import io.opentelemetry.kotlin.semconv.ServerAttributes
import io.opentelemetry.sdk.trace.data.SpanData
import java.net.IDN

/**
 * Type-safe config DSL that limits which hosts produce HTTP client spans.
 *
 * Every host is kept by default. Naming a host turns this into an allowlist: HTTP client
 * spans recording any other host are dropped before export.
 *
 * Only spans are filtered. HTTP client metrics, websocket events and trace context
 * propagation are unaffected.
 *
 * ```kotlin
 * instrumentations {
 *     httpTelemetry {
 *         onlyHosts("api.example.com")
 *     }
 * }
 * ```
 */
@OpenTelemetryDslMarker
class HttpTelemetryConfiguration internal constructor() {
    private val allowedHosts = mutableSetOf<String>()

    /**
     * Keeps HTTP client spans only for these hosts.
     *
     * Host names are compared in full, ignoring case. Pass a bare ASCII host name: a
     * scheme, port, path or user info is rejected.
     *
     * An internationalized host must be given in its punycode form, because HTTP clients
     * disagree about how to derive it.
     *
     * @throws IllegalArgumentException if a host is not a bare ASCII host name.
     */
    fun onlyHosts(vararg hosts: String) {
        hosts.forEach { allowedHosts.add(validatedHost(it)) }
    }

    /** True while no host has been named, so that filtering can be skipped entirely. */
    internal fun keepsEveryHost(): Boolean = allowedHosts.isEmpty()

    /**
     * Only HTTP client spans are considered. gRPC and database spans also record
     * `server.address`, and a span that records no host at all is always kept.
     */
    internal fun rejects(span: SpanData): Boolean {
        if (keepsEveryHost() || span.kind != SpanKind.CLIENT) {
            return false
        }
        val attributes = span.attributes
        if (attributes.get(HTTP_REQUEST_METHOD) == null) {
            return false
        }
        val host = attributes.get(SERVER_ADDRESS) ?: return false
        // A host with no punycode form cannot be one that was configured, since
        // configuration rejects those, so it is not on the allowlist.
        val candidate = punycode(host.lowercase()) ?: return true
        return candidate !in allowedHosts
    }

    private companion object {
        private const val MAX_ASCII = 127

        /** Set by the HTTP client instrumentations. */
        private val SERVER_ADDRESS = AttributeKey.stringKey(ServerAttributes.SERVER_ADDRESS)

        /** Required on every HTTP span, so its absence means the span is not HTTP. */
        private val HTTP_REQUEST_METHOD = AttributeKey.stringKey(HttpAttributes.HTTP_REQUEST_METHOD)

        /** Characters that cannot appear in a recorded host, so a pattern using one never matches. */
        private const val ILLEGAL_HOST_CHARS = "/?#@:*"

        /**
         * Rejects a host that could never match a recorded one.
         */
        fun validatedHost(value: String): String {
            val host = value.trim().lowercase()
            val bare =
                host.isNotEmpty() &&
                    host.all { it.code <= MAX_ASCII } &&
                    host.none { it.isWhitespace() || it in ILLEGAL_HOST_CHARS }
            require(bare) {
                "Invalid host name '$value'; expected a bare ASCII host name such as " +
                    "api.example.com, or the punycode form of an internationalized host, " +
                    "such as xn--bcher-kva.example"
            }
            return host
        }

        /**
         * Converts a recorded host to punycode, because `HttpURLConnection` reports whatever
         * host the caller wrote while configured hosts are always ASCII.
         *
         * A host that is already ASCII is returned unchanged rather than round-tripped, so
         * this normalizes spelling without imposing IDN label rules on it. Returns null for a
         * host with no punycode form.
         */
        fun punycode(host: String): String? =
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
