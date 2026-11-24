/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.connectivity.Compression

/** Overrides the global gRPC export settings for one signal. */
@OpenTelemetryDslMarker
class GrpcEndpointConfiguration internal constructor() {
    /** Collector URL, used without appending an HTTP signal path. Blank uses the global URL. */
    var url: String = ""

    /** Signal-specific headers. Global static headers and supplied headers take precedence. */
    var headers: Map<String, String> = emptyMap()

    /** Compression override. Null uses the global compression setting. */
    var compression: Compression? = null
}
