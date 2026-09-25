/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import java.util.function.Predicate

/**
 * Type-safe config DSL that controls which HTTP telemetry is reported.
 *
 * ```kotlin
 * instrumentations {
 *     httpTelemetry {
 *         shouldRecordSpanForHost { host -> host == "api.example.com" }
 *     }
 * }
 * ```
 */
@OpenTelemetryDslMarker
class HttpTelemetryConfiguration internal constructor() {
    private var recordSpanForHost: Predicate<String>? = null

    /**
     * Decides, per host, whether to record an HTTP client span. Return true to keep the
     * span and false to drop it, so the [predicate] can express either an allowlist or a
     * denylist:
     *
     * ```kotlin
     * shouldRecordSpanForHost { it == "api.example.com" }           // allowlist
     * shouldRecordSpanForHost { it != "analytics.example.net" }     // denylist
     * ```
     *
     * Every host is recorded while no predicate is set, and the last predicate set wins.
     *
     * The host is the `server.address` of the span, lowercased, exactly as the client
     * recorded it. Note that OkHttp canonicalizes an internationalized host to punycode
     * while `HttpURLConnection` reports whatever host the caller wrote, so a predicate
     * covering such a host has to accept both spellings.
     *
     * Only spans are affected. HTTP client metrics, websocket events and trace context
     * propagation are unaffected.
     */
    @Incubating
    fun shouldRecordSpanForHost(predicate: Predicate<String>) {
        recordSpanForHost = predicate
    }

    internal fun recordSpanForHost(): Predicate<String>? = recordSpanForHost
}
