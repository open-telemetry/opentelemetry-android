/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

interface EndpointConnectivity {
    fun getUrl(): String

    fun getHeaders(): Map<String, String>
}
