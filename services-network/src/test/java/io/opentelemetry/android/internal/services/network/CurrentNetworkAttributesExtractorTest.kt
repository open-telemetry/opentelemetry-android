/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.NetworkAttributes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(IncubatingApi::class)
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
                AttributeKey.stringKey(NetworkAttributes.NETWORK_CONNECTION_TYPE) to "cell",
                AttributeKey.stringKey(NetworkAttributes.NETWORK_CONNECTION_SUBTYPE) to "aaa",
                AttributeKey.stringKey(NetworkAttributes.NETWORK_CARRIER_NAME) to "ShadyTel",
                AttributeKey.stringKey(NetworkAttributes.NETWORK_CARRIER_ICC) to "US",
                AttributeKey.stringKey(NetworkAttributes.NETWORK_CARRIER_MCC) to "usa",
                AttributeKey.stringKey(NetworkAttributes.NETWORK_CARRIER_MNC) to "omg",
            )
        assertThat(attributes).isEqualTo(expected)
    }

    @Test
    fun getNetworkAttributes_withoutCarrier() {
        val currentNetwork = CurrentNetwork(state = NetworkState.TRANSPORT_CELLULAR, subType = "aaa")

        val attributes = underTest.extract(currentNetwork).asMap()
        val expected =
            mapOf(
                AttributeKey.stringKey(NetworkAttributes.NETWORK_CONNECTION_SUBTYPE) to "aaa",
                AttributeKey.stringKey(NetworkAttributes.NETWORK_CONNECTION_TYPE) to "cell",
            )
        assertThat(attributes).isEqualTo(expected)
    }
}
