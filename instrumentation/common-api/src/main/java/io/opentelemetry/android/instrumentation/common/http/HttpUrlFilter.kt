/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common.http

import android.net.Uri

/**
 * Decides whether an outgoing HTTP request should be instrumented.
 *
 * HTTP instrumentations consult this before starting an operation, so a request that is
 * filtered out produces no span, no HTTP client metrics, and no injected trace context.
 *
 * Implementations must be thread-safe and must not block; they are called on the calling
 * thread of every HTTP request.
 */
fun interface HttpUrlFilter {
    /**
     * @param url The full request URL, for example `https://api.example.com/v1/items?q=1`.
     * @return true when telemetry should be produced for this request.
     */
    fun shouldInstrument(url: String): Boolean

    companion object {
        /**
         * Instruments every request. This is the default for all HTTP instrumentations, and
         * always returns the same instance so that callers can detect it by identity and skip
         * filtering altogether.
         */
        @JvmStatic
        fun instrumentAll(): HttpUrlFilter = InstrumentAll

        /**
         * Instruments only requests whose host matches one of [hosts], for example
         * `onlyHosts("api.example.com", "*.internal.example.com")`.
         *
         * A host is matched exactly, or, when the pattern begins with `*.`, against any
         * subdomain of the remainder: `*.example.com` matches `a.example.com` and
         * `a.b.example.com`, but not `example.com` itself. Pass both to cover the apex.
         * Matching is case-insensitive and ignores the port, path and query of the request.
         *
         * IPv6 literal hosts are not supported and are rejected as patterns, because the same
         * address has several textual spellings and a request could use a different one from
         * the pattern. A request to an IPv6 literal is treated as having no determinable host.
         *
         * A URL whose host cannot be determined is not instrumented.
         *
         * @throws IllegalArgumentException if [hosts] is empty or contains a malformed pattern.
         */
        @JvmStatic
        fun onlyHosts(vararg hosts: String): HttpUrlFilter = HostFilter(hosts, instrumentOnMatch = true)

        /**
         * Instruments every request except those whose host matches one of [hosts]. Patterns
         * are interpreted exactly as in [onlyHosts].
         *
         * A URL whose host cannot be determined is instrumented.
         *
         * @throws IllegalArgumentException if [hosts] is empty or contains a malformed pattern.
         */
        @JvmStatic
        fun exceptHosts(vararg hosts: String): HttpUrlFilter = HostFilter(hosts, instrumentOnMatch = false)
    }
}

private object InstrumentAll : HttpUrlFilter {
    override fun shouldInstrument(url: String): Boolean = true

    override fun toString(): String = "HttpUrlFilter.instrumentAll()"
}

private class HostFilter(
    patterns: Array<out String>,
    private val instrumentOnMatch: Boolean,
) : HttpUrlFilter {
    private val exactHosts: Set<String>

    /** Subdomain patterns stored as the suffix they must match, e.g. `.example.com`. */
    private val subdomainSuffixes: List<String>

    init {
        require(patterns.isNotEmpty()) { "At least one host pattern is required" }
        val exact = mutableSetOf<String>()
        val suffixes = mutableListOf<String>()
        for (pattern in patterns) {
            val host = validated(pattern)
            if (host.startsWith(WILDCARD_PREFIX)) {
                suffixes.add(host.substring(WILDCARD_PREFIX.length - 1))
            } else {
                exact.add(host)
            }
        }
        exactHosts = exact
        subdomainSuffixes = suffixes
    }

    override fun shouldInstrument(url: String): Boolean {
        val host = hostOf(url) ?: return !instrumentOnMatch
        return matches(host) == instrumentOnMatch
    }

    private fun matches(host: String): Boolean = host in exactHosts || subdomainSuffixes.any { host.endsWith(it) }

    override fun toString(): String {
        val name = if (instrumentOnMatch) "onlyHosts" else "exceptHosts"
        return "HttpUrlFilter.$name(${(exactHosts + subdomainSuffixes.map { "*$it" }).joinToString()})"
    }

    private companion object {
        private const val WILDCARD_PREFIX = "*."

        fun hostOf(url: String): String? {
            val host = Uri.parse(url).host?.lowercase() ?: return null
            // Uri.getHost() truncates an IPv6 literal at the first ':' inside the brackets, and
            // IPv6 literals are rejected as patterns, so such a URL has no host worth matching.
            return if (host.startsWith('[')) null else host
        }

        /**
         * Rejects patterns at configuration time rather than silently never matching at
         * request time.
         */
        fun validated(pattern: String): String {
            val host = pattern.trim().lowercase()
            require(host.isNotEmpty()) { "Host pattern must not be blank" }
            require(host.none { it.isWhitespace() }) { "Host pattern '$pattern' must not contain whitespace" }
            require(!host.contains('/') && !host.contains('?') && !host.contains('#')) {
                "Host pattern '$pattern' must be a host, not a URL"
            }
            require(!host.contains('@')) { "Host pattern '$pattern' must not include user info" }
            require(!host.startsWith('[')) {
                "Host pattern '$pattern' must not be an IPv6 literal; IPv6 hosts are not supported"
            }
            require(!host.contains(':')) {
                "Host pattern '$pattern' must not include a port; IPv6 hosts are not supported"
            }
            val withoutWildcard = host.removePrefix(WILDCARD_PREFIX)
            require(!withoutWildcard.contains('*')) {
                "Host pattern '$pattern' may only use '*' as a leading '*.' label"
            }
            require(withoutWildcard.isNotEmpty()) { "Host pattern '$pattern' must name a domain after '*.'" }
            return host
        }
    }
}
