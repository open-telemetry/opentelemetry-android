/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.features.networkattributes

import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes

class CurrentNetworkAttributesExtractor {
    fun extract(network: CurrentNetwork): Attributes {
        val builder = Attributes.builder()
        network.state.humanName.let {
            builder.put(NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE, it)
        }
        network.subType.let {
            builder.put(NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE, it)
        }
        network.carrierName.let {
            builder.put(NetworkIncubatingAttributes.NETWORK_CARRIER_NAME, it)
        }
        network.carrierCountryCode.let {
            builder.put(NetworkIncubatingAttributes.NETWORK_CARRIER_MCC, it)
        }
        network.carrierNetworkCode.let {
            builder.put(NetworkIncubatingAttributes.NETWORK_CARRIER_MNC, it)
        }
        network.carrierIsoCountryCode.let {
            builder.put(NetworkIncubatingAttributes.NETWORK_CARRIER_ICC, it)
        }
        return builder.build()
    }
}
