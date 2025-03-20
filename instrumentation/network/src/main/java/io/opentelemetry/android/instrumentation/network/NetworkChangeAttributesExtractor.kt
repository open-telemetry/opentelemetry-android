/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network

import io.opentelemetry.android.common.internal.features.networkattributes.CurrentNetworkAttributesExtractor
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes

internal class NetworkChangeAttributesExtractor : NetworkAttributesExtractor {
    private val networkAttributesExtractor = CurrentNetworkAttributesExtractor()

    override fun invoke(
        attributesBuilder: AttributesBuilder,
        currentNetwork: CurrentNetwork,
    ) {
        val status =
            if (currentNetwork.state == NetworkState.NO_NETWORK_AVAILABLE) {
                "lost"
            } else {
                "available"
            }
        attributesBuilder.put(NETWORK_STATUS_KEY, status)
        if (currentNetwork.state == NetworkState.NO_NETWORK_AVAILABLE) {
            attributesBuilder.put(
                NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE,
                currentNetwork.state.humanName,
            )
        } else {
            attributesBuilder.putAll(networkAttributesExtractor.extract(currentNetwork))
        }
    }
}
