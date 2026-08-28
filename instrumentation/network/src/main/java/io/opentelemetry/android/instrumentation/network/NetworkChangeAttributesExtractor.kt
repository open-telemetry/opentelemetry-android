/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network

import io.opentelemetry.android.semconv.NetworkAttributes.NETWORK_STATUS_KEY
import io.opentelemetry.android.semconv.internal.SemconvCompat
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.NetworkAttributes

internal class NetworkChangeAttributesExtractor : NetworkAttributesExtractor {
    private val networkAttributesExtractor = CurrentNetworkAttributesExtractor()

    @OptIn(IncubatingApi::class)
    @Suppress("DEPRECATION")
    override fun invoke(
        attributesBuilder: AttributesBuilder,
        currentNetwork: CurrentNetwork,
    ) {
        if (!SemconvCompat.useLatestExperimental) {
            val status =
                if (currentNetwork.state == NetworkState.NO_NETWORK_AVAILABLE) {
                    "lost"
                } else {
                    "available"
                }
            attributesBuilder.put(NETWORK_STATUS_KEY, status)
        }
        if (currentNetwork.state == NetworkState.NO_NETWORK_AVAILABLE) {
            attributesBuilder.put(
                NetworkAttributes.NETWORK_CONNECTION_TYPE,
                currentNetwork.state.humanName,
            )
        } else {
            attributesBuilder.putAll(networkAttributesExtractor.extract(currentNetwork))
        }
    }
}
