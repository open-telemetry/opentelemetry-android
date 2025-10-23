/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

/**
 * Type-safe config DSL that controls the endpoint that HTTP exports of telemetry are sent to.
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
)
