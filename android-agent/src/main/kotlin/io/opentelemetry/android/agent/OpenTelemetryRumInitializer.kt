/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.connectivity.EndpointConnectivity
import io.opentelemetry.android.agent.connectivity.HttpEndpointConnectivity
import io.opentelemetry.android.config.OtelRumConfig

object OpenTelemetryRumInitializer {
    @JvmStatic
    fun initialize(
        application: Application,
        endpointBaseUrl: String,
        endpointHeaders: Map<String, String> = emptyMap(),
        spanEndpointConnectivity: EndpointConnectivity =
            HttpEndpointConnectivity.forTraces(
                endpointBaseUrl,
                endpointHeaders,
            ),
        logEndpointConnectivity: EndpointConnectivity =
            HttpEndpointConnectivity.forLogs(
                endpointBaseUrl,
                endpointHeaders,
            ),
        metricEndpointConnectivity: EndpointConnectivity =
            HttpEndpointConnectivity.forMetrics(
                endpointBaseUrl,
                endpointHeaders,
            ),
        rumConfig: OtelRumConfig = OtelRumConfig(),
    ): OpenTelemetryRum {
        TODO()
    }
}
