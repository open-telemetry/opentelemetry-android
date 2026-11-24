/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.OtlpTransport
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

internal object EndpointUrlValidator {
    fun validate(
        transport: OtlpTransport,
        endpoint: String,
    ) {
        val uri =
            try {
                URI(endpoint)
            } catch (e: URISyntaxException) {
                throw IllegalArgumentException("Invalid endpoint, must be a URL: $endpoint", e)
            }
        val hasHost =
            uri.host != null ||
                try {
                    URL(endpoint).host.isNotEmpty()
                } catch (_: MalformedURLException) {
                    false
                }
        require((uri.scheme == "http" || uri.scheme == "https") && hasHost) {
            "Invalid ${transport.name.lowercase()} endpoint, must start with http:// or https://: $endpoint"
        }
    }
}
