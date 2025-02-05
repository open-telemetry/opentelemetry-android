/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider.UNKNOWN_NETWORK
import io.opentelemetry.android.internal.services.network.detector.NetworkDetector
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(
    maxSdk = Build.VERSION_CODES.S,
)
internal class CurrentNetworkProviderTest {
    private val fakeNet: Network = mockk()
    private val wifi = CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build()
    private val cellular =
        CurrentNetwork
            .builder(NetworkState.TRANSPORT_CELLULAR)
            .subType("LTE")
            .build()
    private val noNetwork = CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build()

    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP)
    fun lollipop() {
        val networkRequest: NetworkRequest = mockk()
        val networkDetector: NetworkDetector = mockk()
        val connectivityManager: ConnectivityManager = mockk()

        every { networkDetector.detectCurrentNetwork() } returns wifi andThen cellular

        val currentNetworkProvider =
            CurrentNetworkProvider(networkDetector, connectivityManager) { networkRequest }

        assertThat(
            CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build(),
        ).isEqualTo(
            currentNetworkProvider.currentNetwork,
        )

        val monitorSlot = slot<NetworkCallback>()

        verify {
            connectivityManager.registerNetworkCallback(networkRequest, capture(monitorSlot))
        }

        val notified = AtomicInteger(0)
        currentNetworkProvider.addNetworkChangeListener { currentNetwork: CurrentNetwork? ->
            val timesCalled = notified.incrementAndGet()
            if (timesCalled == 1) {
                assertThat(currentNetwork).isEqualTo(cellular)
            } else {
                assertThat(currentNetwork).isEqualTo(noNetwork)
            }
        }
        // note: we ignore the network passed in and just rely on refreshing the network info when
        // this is happens
        monitorSlot.captured.onAvailable(fakeNet)
        assertThat(notified.get()).isEqualTo(1L)
        monitorSlot.captured.onLost(fakeNet)
        assertThat(notified.get()).isEqualTo(2L)
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.S, minSdk = Build.VERSION_CODES.O)
    fun `modern SDKs`() {
        val networkRequest: NetworkRequest = mockk()
        val networkDetector: NetworkDetector = mockk()
        val connectivityManager: ConnectivityManager = mockk()
        every { networkDetector.detectCurrentNetwork() } returns wifi andThen cellular

        val currentNetworkProvider =
            CurrentNetworkProvider(
                networkDetector,
                connectivityManager,
            ) { networkRequest }

        assertThat(currentNetworkProvider.currentNetwork).isEqualTo(wifi)
        verify(exactly = 0) {
            connectivityManager.registerNetworkCallback(any(), any<NetworkCallback>())
        }

        val monitorSlot = slot<NetworkCallback>()
        verify {
            connectivityManager.registerDefaultNetworkCallback(capture(monitorSlot))
        }

        val notified = AtomicInteger(0)
        currentNetworkProvider.addNetworkChangeListener { currentNetwork: CurrentNetwork? ->
            val timesCalled = notified.incrementAndGet()
            if (timesCalled == 1) {
                assertThat(currentNetwork).isEqualTo(cellular)
            } else {
                assertThat(currentNetwork).isEqualTo(noNetwork)
            }
        }
        // note: we ignore the network passed in and just rely on refreshing the network info when
        // this is happens
        monitorSlot.captured.onAvailable(fakeNet)
        assertThat(notified.get()).isEqualTo(1L)
        monitorSlot.captured.onLost(fakeNet)
        assertThat(notified.get()).isEqualTo(2L)
    }

    @Test
    fun `network detector exception`() {
        val networkDetector: NetworkDetector = mockk()
        every { networkDetector.detectCurrentNetwork() }.throws(SecurityException("bug"))

        val currentNetworkProvider =
            CurrentNetworkProvider(networkDetector, Mockito.mock())
        assertThat(currentNetworkProvider.refreshNetworkStatus()).isEqualTo(UNKNOWN_NETWORK)
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.S, minSdk = Build.VERSION_CODES.O)
    fun `network detector exception on callback registration`() {
        val networkDetector: NetworkDetector = mockk()
        val connectivityManager: ConnectivityManager = mockk()
        val networkRequest: NetworkRequest = mockk()

        every { networkDetector.detectCurrentNetwork() } returns wifi
        every { connectivityManager.registerDefaultNetworkCallback(any()) }.throws(
            SecurityException(
                "bug",
            ),
        )

        val currentNetworkProvider =
            CurrentNetworkProvider(networkDetector, connectivityManager) { networkRequest }
        assertThat(currentNetworkProvider.refreshNetworkStatus()).isEqualTo(wifi)
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP)
    fun `network detector exception on callback registration lollipop`() {
        val networkDetector: NetworkDetector = mockk()
        val connectivityManager: ConnectivityManager = mockk()
        val networkRequest: NetworkRequest = mockk()

        every { networkDetector.detectCurrentNetwork() } returns wifi
        every {
            connectivityManager.registerNetworkCallback(
                networkRequest,
                any<NetworkCallback>(),
            )
        }.throws(SecurityException("bug"))

        val currentNetworkProvider =
            CurrentNetworkProvider(networkDetector, connectivityManager) { networkRequest }
        assertThat(currentNetworkProvider.refreshNetworkStatus()).isEqualTo(wifi)
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP)
    fun `should not fail on immediate ConnectionManager call lollipop`() {
        val networkDetector: NetworkDetector = mockk()
        val connectivityManager: ConnectivityManager = mockk()
        val networkRequest: NetworkRequest = mockk()

        every {
            connectivityManager.registerNetworkCallback(
                networkRequest,
                any<NetworkCallback>(),
            )
        }.answers { a ->
            run {
                val x: NetworkCallback = a.invocation.args[1] as NetworkCallback
                x.onAvailable(mockk())
            }
        }

        val networkProvider =
            CurrentNetworkProvider(networkDetector, connectivityManager) { networkRequest }
        assertThat(networkProvider.refreshNetworkStatus()).isEqualTo(UNKNOWN_NETWORK)
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.S, minSdk = Build.VERSION_CODES.O)
    fun `should not fail on immediate ConnectionManager call`() {
        val networkDetector: NetworkDetector = mockk()
        val connectivityManager: ConnectivityManager = mockk()
        val networkRequest: NetworkRequest = mockk()

        every { connectivityManager.registerDefaultNetworkCallback(any<NetworkCallback>()) }
            .answers { a ->
                run {
                    val x: NetworkCallback = a.invocation.args[0] as NetworkCallback
                    x.onAvailable(mockk())
                }
            }

        val networkProvider =
            CurrentNetworkProvider(networkDetector, connectivityManager) { networkRequest }
        assertThat(networkProvider.refreshNetworkStatus()).isEqualTo(UNKNOWN_NETWORK)
    }
}
