/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.endpoint

internal data class DefaultEndpointConfig(
    private val url: String,
    private val headers: Map<String, String>,
) : EndpointConfig {
    override fun getUrl(): String {
        return url
    }

    override fun getHeaders(): Map<String, String> {
        return headers
    }
}
