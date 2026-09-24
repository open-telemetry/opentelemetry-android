/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.NetworkAttributes
import io.opentelemetry.sdk.trace.ReadWriteSpan
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(IncubatingApi::class)
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
                    AttributeKey.stringKey(NetworkAttributes.NETWORK_CONNECTION_TYPE),
                    "cell",
                    AttributeKey.stringKey(NetworkAttributes.NETWORK_CONNECTION_SUBTYPE),
                    "LTE",
                ),
            )
        }
        assertThat(underTest.isEndRequired).isFalse()
    }
}
