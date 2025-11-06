/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

internal interface EndpointConnectivity {
    fun getUrl(): String

    fun getHeaders(): Map<String, String>

    fun getCompression(): Compression
}
