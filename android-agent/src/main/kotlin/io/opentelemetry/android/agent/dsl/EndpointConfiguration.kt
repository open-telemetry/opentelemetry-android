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
     * URL for HTTP export requests.
     */
    var url: String,
    /**
     * Headers that should be attached to HTTP export requests.
     */
    var headers: Map<String, String> = emptyMap(),
    /**
     * Compression algorithm.
     */
    var compression: Compression? = null,
)
