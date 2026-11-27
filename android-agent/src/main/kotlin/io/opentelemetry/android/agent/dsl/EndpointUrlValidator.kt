/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

internal object EndpointUrlValidator {
    fun validate(endpoint: String) {
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
            "Invalid endpoint, must start with http:// or https://: $endpoint"
        }
        val authority = uri.rawAuthority.orEmpty().substringAfterLast('@')
        val portSeparator = authority.lastIndexOf(':')
        if (portSeparator > authority.lastIndexOf(']')) {
            val port = authority.substring(portSeparator + 1).toIntOrNull()
            require(port != null && port in 1..65535) {
                "Invalid endpoint port, must be between 1 and 65535: $endpoint"
            }
        }
    }
}
