/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.Compression

/**
 * Type-safe config DSL that controls the HTTP endpoint for telemetry exports.
 */
@OpenTelemetryDslMarker
class EndpointConfiguration internal constructor(
    /**
     * Base URL for HTTP export requests. The signal-specific path (e.g. /v1/logs) will be
     * appended automatically.
     */
    var url: String,
    /**
     * Full URL for HTTP export requests. When set, this URL is used as-is without appending
     * any signal-specific path. Use this to specify a completely custom endpoint path,
     * for example "https://example.com/v2/logs".
     * Note: when set, this overrides the baseUrl from the surrounding httpExport block entirely.
     */
    var fullUrl: String? = null,
    /**
     * Headers that should be attached to HTTP export requests.
     */
    var headers: Map<String, String> = emptyMap(),
    /**
     * Compression algorithm.
     */
    var compression: Compression? = null,
)
