/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network

import android.app.Application
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.NetworkChangeListener
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
@ExtendWith(MockKExtension::class)
class NetworkChangeInstrumentationTest {
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
        NetworkChangeInstrumentation().install(createInstallationContext())

        verify {
            currentNetworkProvider.addNetworkChangeListener(capture(networkChangeListenerSlot))
        }
        val listener = networkChangeListenerSlot.captured

        listener.onNetworkChange(CurrentNetwork(NetworkState.TRANSPORT_WIFI))

        val events = otelTesting.logRecords
        assertThat(events).hasSize(1)
        val event = events[0]
        assertThat(event)
            .hasEventName("network.change")
            .hasAttributesSatisfying(
                equalTo(NETWORK_STATUS_KEY, "available"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE, "wifi"),
            )
    }

    @Test
    fun networkAvailable_cellular() {
        val networkChangeListenerSlot = slot<NetworkChangeListener>()
        NetworkChangeInstrumentation().install(createInstallationContext())

        verify {
            currentNetworkProvider.addNetworkChangeListener(capture(networkChangeListenerSlot))
        }
        val listener = networkChangeListenerSlot.captured

        val network =
            CurrentNetwork(
                state = NetworkState.TRANSPORT_CELLULAR,
                subType = "LTE",
                carrier = Carrier(206, "ShadyTel", "usa", "omg", "US"),
            )

        listener.onNetworkChange(network)

        val events = otelTesting.logRecords
        assertThat(events).hasSize(1)
        val event = events[0]
        assertThat(event)
            .hasEventName("network.change")
            .hasAttributesSatisfying(
                equalTo(NETWORK_STATUS_KEY, "available"),
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
        NetworkChangeInstrumentation().install(createInstallationContext())

        verify {
            currentNetworkProvider.addNetworkChangeListener(capture(networkChangeListenerSlot))
        }
        val listener = networkChangeListenerSlot.captured

        listener.onNetworkChange(CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE))

        val events = otelTesting.logRecords
        assertThat(events).hasSize(1)
        val event = events[0]
        assertThat(event)
            .hasEventName("network.change")
            .hasAttributesSatisfying(
                equalTo(NETWORK_STATUS_KEY, "lost"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE, "unavailable"),
            )
    }

    @Test
    fun noEventsPlease() {
        val networkChangeListenerSlot = slot<NetworkChangeListener>()
        val appStateListener = slot<ApplicationStateListener>()
        NetworkChangeInstrumentation().install(createInstallationContext())

        verify {
            currentNetworkProvider.addNetworkChangeListener(capture(networkChangeListenerSlot))
            appLifecycle.registerListener(capture(appStateListener))
        }
        val networkListener = networkChangeListenerSlot.captured
        val applicationListener = appStateListener.captured

        applicationListener.onApplicationBackgrounded()

        networkListener.onNetworkChange(
            CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE),
        )
        assertThat(otelTesting.logRecords).isEmpty()
        networkListener.onNetworkChange(
            CurrentNetwork(state = NetworkState.TRANSPORT_CELLULAR, subType = "LTE"),
        )
        assertThat(otelTesting.logRecords).isEmpty()

        applicationListener.onApplicationForegrounded()

        networkListener.onNetworkChange(
            CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE),
        )
        assertThat(otelTesting.logRecords).hasSize(1)
        val event = otelTesting.logRecords[0]
        assertThat(event)
            .hasEventName("network.change")
            .hasAttributesSatisfying(
                equalTo(NETWORK_STATUS_KEY, "lost"),
                equalTo(NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE, "unavailable"),
            )
    }

    private fun createInstallationContext(): InstallationContext {
        val app = mockk<Application>()
        val services = mockk<Services>()
        every { services.currentNetworkProvider } returns currentNetworkProvider
        every { services.appLifecycle } returns appLifecycle
        Services.set(services)
        return InstallationContext(app, otelTesting.openTelemetry, mockk<SessionProvider>(relaxed = true))
    }
}
