/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.features.networkattributes

import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentNetworkAttributesExtractorTest {
    private val underTest: CurrentNetworkAttributesExtractor = CurrentNetworkAttributesExtractor()

    @Test
    fun getNetworkAttributes_withCarrier() {
        val currentNetwork =
            CurrentNetwork(
                state = NetworkState.TRANSPORT_CELLULAR,
                subType = "aaa",
                carrier = Carrier(206, "ShadyTel", "usa", "omg", "US"),
            )

        val attributes = underTest.extract(currentNetwork).asMap()
        val expected =
            mapOf(
                NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE to "cell",
                NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE to "aaa",
                NetworkIncubatingAttributes.NETWORK_CARRIER_NAME to "ShadyTel",
                NetworkIncubatingAttributes.NETWORK_CARRIER_ICC to "US",
                NetworkIncubatingAttributes.NETWORK_CARRIER_MCC to "usa",
                NetworkIncubatingAttributes.NETWORK_CARRIER_MNC to "omg",
            )
        assertEquals(expected, attributes)
    }

    @Test
    fun getNetworkAttributes_withoutCarrier() {
        val currentNetwork = CurrentNetwork(state = NetworkState.TRANSPORT_CELLULAR, subType = "aaa")

        val attributes = underTest.extract(currentNetwork).asMap()
        val expected =
            mapOf(
                NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE to "aaa",
                NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE to "cell",
            )
        assertEquals(expected, attributes)
    }
}
