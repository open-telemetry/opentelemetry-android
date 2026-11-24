/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.android.agent.connectivity.EndpointConnectivity

internal enum class OtlpTransport {
    HTTP,
    GRPC,
}

internal data class ResolvedExportConfiguration(
    val transport: OtlpTransport,
    val spans: EndpointConnectivity,
    val logs: EndpointConnectivity,
    val metrics: EndpointConnectivity,
)
