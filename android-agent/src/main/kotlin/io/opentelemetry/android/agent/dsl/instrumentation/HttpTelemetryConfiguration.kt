/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import android.util.Log
import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.common.RumConstants

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
    private val allowed = mutableSetOf<String>()

    /**
     * Keeps HTTP client spans only for these hosts.
     *
     * Host names are compared in full, ignoring case. Pass a bare ASCII host name; a value
     * carrying a scheme, port, path or user info is ignored with a warning, rather than
     * failing initialization. An internationalized host must be given in its punycode form,
     * because HTTP clients disagree about how to derive it.
     */
    fun onlyHosts(vararg hosts: String) {
        for (value in hosts) {
            val host = value.trim().lowercase()
            if (isBareAsciiHost(host)) {
                allowed.add(host)
            } else {
                Log.w(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "Ignoring host name '$value'; expected a bare ASCII host name",
                )
            }
        }
    }

    /**
     * A snapshot of the configured hosts. Filtering reads this once, so later edits through
     * a retained DSL reference cannot race with the exporter thread.
     */
    internal fun allowedHosts(): Set<String> = allowed.toSet()

    private companion object {
        private const val MAX_ASCII = 127

        /** Characters that cannot appear in a recorded host, so a value using one never matches. */
        private const val ILLEGAL_HOST_CHARS = "/?#@:*"

        fun isBareAsciiHost(host: String): Boolean =
            host.isNotEmpty() &&
                host.all { it.code <= MAX_ASCII } &&
                host.none { it.isWhitespace() || it in ILLEGAL_HOST_CHARS }
    }
}
