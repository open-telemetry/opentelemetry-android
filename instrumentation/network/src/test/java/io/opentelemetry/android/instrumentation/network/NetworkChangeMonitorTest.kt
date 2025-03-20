/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network

import android.os.Build
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.NetworkChangeListener
import io.opentelemetry.android.test.common.hasEventName
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.sdk.logs.data.internal.ExtendedLogRecordData
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.function.BiConsumer

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
@ExtendWith(MockKExtension::class)
class NetworkChangeMonitorTest {
    private lateinit var otelTesting: OpenTelemetryRule

    @MockK
    lateinit var currentNetworkProvider: CurrentNetworkProvider

    @MockK
    lateinit var appLifecycle: AppLifecycle

    @Before
    fun setUp() {
        otelTesting = OpenTelemetryRule.create()
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun networkAvailable_wifi() {
        val networkChangeListenerSlot = slot<NetworkChangeListener>()
        create().start()

        verify {
            currentNetworkProvider.addNetworkChangeListener(capture(networkChangeListenerSlot))
        }
        val listener = networkChangeListenerSlot.captured

        listener.onNetworkChange(CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build())

        val events = otelTesting.logRecords
        assertThat(events).hasSize(1)
        val event = events[0] as ExtendedLogRecordData
        assertThat(event)
            .hasEventName("network.change")
            .hasAttributesSatisfyingExactly(
                equalTo(NetworkApplicationListener.NETWORK_STATUS_KEY, "available"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE, "wifi"),
            )
    }

    @Test
    fun networkAvailable_cellular() {
        val networkChangeListenerSlot = slot<NetworkChangeListener>()
        create().start()
        verify {
            currentNetworkProvider.addNetworkChangeListener(capture(networkChangeListenerSlot))
        }
        val listener = networkChangeListenerSlot.captured

        val network =
            CurrentNetwork
                .builder(NetworkState.TRANSPORT_CELLULAR)
                .subType("LTE")
                .carrier(Carrier(206, "ShadyTel", "usa", "omg", "US"))
                .build()

        listener.onNetworkChange(network)

        val events = otelTesting.logRecords
        assertThat(events).hasSize(1)
        val event = events[0] as ExtendedLogRecordData
        assertThat(event)
            .hasEventName("network.change")
            .hasAttributesSatisfyingExactly(
                equalTo(NetworkApplicationListener.NETWORK_STATUS_KEY, "available"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE, "cell"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE, "LTE"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CARRIER_NAME, "ShadyTel"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CARRIER_ICC, "US"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CARRIER_MCC, "usa"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CARRIER_MNC, "omg"),
            )
    }

    @Test
    fun networkLost() {
        val networkChangeListenerSlot = slot<NetworkChangeListener>()
        create().start()

        verify {
            currentNetworkProvider.addNetworkChangeListener(capture(networkChangeListenerSlot))
        }
        val listener = networkChangeListenerSlot.captured

        listener.onNetworkChange(CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build())

        val events = otelTesting.logRecords
        assertThat(events).hasSize(1)
        val event = events[0] as ExtendedLogRecordData
        assertThat(event)
            .hasEventName("network.change")
            .hasAttributesSatisfyingExactly(
                equalTo(NetworkApplicationListener.NETWORK_STATUS_KEY, "lost"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE, "unavailable"),
            )
    }

    @Test
    fun noEventsPlease() {
        val networkChangeListenerSlot = slot<NetworkChangeListener>()
        val appStateListener = slot<ApplicationStateListener>()
        create().start()

        verify {
            currentNetworkProvider.addNetworkChangeListener(capture(networkChangeListenerSlot))
            appLifecycle.registerListener(capture(appStateListener))
        }
        val networkListener = networkChangeListenerSlot.captured
        val applicationListener = appStateListener.captured

        applicationListener.onApplicationBackgrounded()

        networkListener.onNetworkChange(
            CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build(),
        )
        assertThat(otelTesting.logRecords).isEmpty()
        networkListener.onNetworkChange(
            CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR).subType("LTE").build(),
        )
        assertThat(otelTesting.logRecords).isEmpty()

        applicationListener.onApplicationForegrounded()

        networkListener.onNetworkChange(
            CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build(),
        )
        assertThat(otelTesting.logRecords).hasSize(1)
        val event: ExtendedLogRecordData = otelTesting.logRecords[0] as ExtendedLogRecordData
        assertThat(event)
            .hasEventName("network.change")
            .hasAttributesSatisfyingExactly(
                equalTo(NetworkApplicationListener.NETWORK_STATUS_KEY, "lost"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE, "unavailable"),
            )
    }

    private fun create(): NetworkChangeMonitor =
        NetworkChangeMonitor(
            otelTesting.openTelemetry,
            appLifecycle,
            currentNetworkProvider,
            listOf<BiConsumer<AttributesBuilder, CurrentNetwork>>(NetworkChangeAttributesExtractor()),
        )
}
