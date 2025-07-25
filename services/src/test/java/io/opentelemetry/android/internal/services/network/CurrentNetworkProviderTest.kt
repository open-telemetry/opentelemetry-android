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
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.N, Build.VERSION_CODES.P, Build.VERSION_CODES.Q, Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
internal class CurrentNetworkProviderTest {
    private val fakeNet: Network = mockk()
    private val wifi = CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build()
    private val cellular =
        CurrentNetwork
            .builder(NetworkState.TRANSPORT_CELLULAR)
            .subType("LTE")
            .build()
    private val noNetwork = CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build()
    private val mockConnectivityManager = mockk<ConnectivityManager>()
    private val errorNetworkDetector: NetworkDetector =
        mockk<NetworkDetector>().apply {
            every { detectCurrentNetwork() }.throws(SecurityException("bug"))
        }

    @Test
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    fun `verify current network provider on lollipop`() {
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
    fun `verify current network provider`() {
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
        val currentNetworkProvider = CurrentNetworkProvider(errorNetworkDetector, mockConnectivityManager)
        assertThat(currentNetworkProvider.refreshNetworkStatus()).isEqualTo(UNKNOWN_NETWORK)
    }

    @Test
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
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
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
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
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
