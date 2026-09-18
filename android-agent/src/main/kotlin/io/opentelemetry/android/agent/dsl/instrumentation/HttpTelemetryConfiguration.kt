/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.kotlin.semconv.ServerAttributes
import io.opentelemetry.sdk.trace.data.SpanData
import java.net.IDN

/**
 * Type-safe config DSL that limits which hosts produce HTTP telemetry.
 *
 * Every host produces telemetry by default. Naming a host turns this into an allowlist:
 * telemetry that records any other host is dropped.
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
     * Keeps HTTP telemetry only for these hosts.
     *
     * Host names are compared in full, ignoring case. Pass a bare host name: a scheme,
     * port, path or user info is rejected. An internationalized host is converted to
     * its punycode form, because that is the form the HTTP clients record.
     *
     * @throws IllegalArgumentException if a host is not a bare host name.
     */
    fun onlyHosts(vararg hosts: String) {
        hosts.forEach { allowedHosts.add(validatedHost(it)) }
    }

    /** True while no host has been named, so that filtering can be skipped entirely. */
    internal fun keepsEveryHost(): Boolean = allowedHosts.isEmpty()

    /**
     * Telemetry that records no host is always kept, so the allowlist only decides about
     * spans carrying `server.address`.
     */
    internal fun rejects(span: SpanData): Boolean {
        if (keepsEveryHost()) {
            return false
        }
        val host = span.attributes.get(SERVER_ADDRESS) ?: return false
        val candidate = punycode(host.lowercase()) ?: return true
        return candidate !in allowedHosts
    }

    private companion object {
        private const val MAX_ASCII = 127

        /** Set by the HTTP client instrumentations. */
        private val SERVER_ADDRESS = AttributeKey.stringKey(ServerAttributes.SERVER_ADDRESS)

        /** Characters that cannot appear in a recorded host, so a pattern using one never matches. */
        private const val ILLEGAL_HOST_CHARS = "/?#@:*"

        /**
         * Rejects a host that could never match a recorded one.
         */
        fun validatedHost(value: String): String {
            val host = value.trim().lowercase()
            val bare = host.isNotEmpty() && host.none { it.isWhitespace() || it in ILLEGAL_HOST_CHARS }
            return requireNotNull(if (bare) punycode(host) else null) {
                "Invalid host name '$value'; expected a bare host name such as api.example.com"
            }
        }

        /**
         * Converts a host to the punycode spelling the HTTP clients compare against.
         *
         * OkHttp canonicalizes with `IDN.toASCII`, so it always records punycode, while
         * `HttpURLConnection` records whatever host the caller wrote. Both sides of the
         * comparison are converted so that the two agree.
         *
         * A host that is already ASCII is returned unchanged rather than round-tripped, so
         * this normalizes spelling without also imposing IDN label rules on it. Returns null
         * for a host with no punycode form, which callers treat according to whether the
         * host came from configuration or from recorded telemetry.
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
