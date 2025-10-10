/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

@OpenTelemetryDslMarker
class EndpointConfiguration internal constructor(
    var url: String,
    var headers: Map<String, String> = emptyMap(),
)
