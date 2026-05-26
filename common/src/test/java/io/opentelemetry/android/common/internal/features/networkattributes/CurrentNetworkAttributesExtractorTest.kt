/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.features.networkattributes

import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NETWORK_CARRIER_ICC
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NETWORK_CARRIER_MCC
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NETWORK_CARRIER_MNC
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NETWORK_CARRIER_NAME
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NETWORK_CONNECTION_SUBTYPE
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NETWORK_CONNECTION_TYPE
import org.junit.Assert.assertEquals
import org.junit.Test

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
                stringKey(NETWORK_CONNECTION_TYPE) to "cell",
                stringKey(NETWORK_CONNECTION_SUBTYPE) to "aaa",
                stringKey(NETWORK_CARRIER_NAME) to "ShadyTel",
                stringKey(NETWORK_CARRIER_ICC) to "US",
                stringKey(NETWORK_CARRIER_MCC) to "usa",
                stringKey(NETWORK_CARRIER_MNC) to "omg",
            )
        assertEquals(expected, attributes)
    }

    @Test
    fun getNetworkAttributes_withoutCarrier() {
        val currentNetwork = CurrentNetwork(state = NetworkState.TRANSPORT_CELLULAR, subType = "aaa")

        val attributes = underTest.extract(currentNetwork).asMap()
        val expected =
            mapOf(
                stringKey(NETWORK_CONNECTION_SUBTYPE) to "aaa",
                stringKey(NETWORK_CONNECTION_TYPE) to "cell",
            )
        assertEquals(expected, attributes)
    }
}
