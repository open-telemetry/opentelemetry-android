/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.networkattrs

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class NetworkAttributesSpanAppenderTest {
    @MockK
    lateinit var currentNetworkProvider: CurrentNetworkProvider

    @MockK
    lateinit var span: ReadWriteSpan

    @InjectMockKs
    lateinit var underTest: NetworkAttributesSpanAppender

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { span.setAllAttributes(any()) } returns span
    }

    @Test
    fun shouldAppendNetworkAttributes() {
        val network = CurrentNetwork(state = NetworkState.TRANSPORT_CELLULAR, subType = "LTE")
        every { currentNetworkProvider.currentNetwork } returns network
        assertThat(underTest.isStartRequired).isTrue()

        underTest.onStart(Context.current(), span)

        verify {
            span.setAllAttributes(
                Attributes.of(
                    NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE,
                    "cell",
                    NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE,
                    "LTE",
                ),
            )
        }
        assertThat(underTest.isEndRequired).isFalse()
    }
}
