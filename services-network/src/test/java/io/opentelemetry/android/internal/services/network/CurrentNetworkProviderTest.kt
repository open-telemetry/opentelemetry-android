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
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider.Companion.UNKNOWN_NETWORK
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.N, Build.VERSION_CODES.P, Build.VERSION_CODES.Q, Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
internal class CurrentNetworkProviderTest {
    private val fakeNet: Network = mockk()
    private val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
    private val cellular =
        CurrentNetwork(
            state = NetworkState.TRANSPORT_CELLULAR,
            subType = "LTE",
        )
    private val cellularWithoutSubtype = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR)
    private val noNetwork = CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE)
    private val mockConnectivityManager = mockk<ConnectivityManager>()
    private val networkCallback = slot<NetworkCallback>()
    private val errorNetworkDetector: NetworkDetector =
        mockk<NetworkDetector>().apply {
            every { detectCurrentNetwork() }.throws(SecurityException("bug"))
        }

    @Test
    fun `verify current network provider`() {
        val networkRequest: NetworkRequest = mockk()
        val networkDetector: NetworkDetector = mockk()
        val connectivityManager: ConnectivityManager = mockk()
        every { networkDetector.detectCurrentNetwork() } returns wifi andThen cellular
        every { connectivityManager.registerDefaultNetworkCallback(any()) } just Runs
        every { connectivityManager.unregisterNetworkCallback(any<NetworkCallback>()) } just Runs

        val currentNetworkProvider =
            CurrentNetworkProviderImpl(
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
        currentNetworkProvider.addNetworkChangeListener { currentNetwork: CurrentNetwork ->
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

        // Verify close
        currentNetworkProvider.close()
        verify {
            connectivityManager.unregisterNetworkCallback(monitorSlot.captured)
        }
    }

    @Test
    fun `remove network change listener`() {
        val networkRequest: NetworkRequest = mockk()
        val networkDetector: NetworkDetector = mockk()
        val connectivityManager: ConnectivityManager = mockk()
        every { networkDetector.detectCurrentNetwork() } returns wifi andThen cellular
        every { connectivityManager.registerDefaultNetworkCallback(any()) } just Runs
        every { connectivityManager.unregisterNetworkCallback(any<NetworkCallback>()) } just Runs

        val currentNetworkProvider =
            CurrentNetworkProviderImpl(
                networkDetector,
                connectivityManager,
            ) { networkRequest }

        val monitorSlot = slot<NetworkCallback>()
        verify {
            connectivityManager.registerDefaultNetworkCallback(capture(monitorSlot))
        }

        val notified = AtomicInteger(0)
        val listener = NetworkChangeListener { notified.incrementAndGet() }
        currentNetworkProvider.addNetworkChangeListener(listener)

        monitorSlot.captured.onAvailable(fakeNet)
        assertThat(notified.get()).isEqualTo(1)

        currentNetworkProvider.removeNetworkChangeListener(listener)

        monitorSlot.captured.onAvailable(fakeNet)
        assertThat(notified.get()).isEqualTo(1)
    }

    @Test
    fun `network detector exception`() {
        val currentNetworkProvider =
            CurrentNetworkProviderImpl(errorNetworkDetector, mockConnectivityManager)
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
            CurrentNetworkProviderImpl(networkDetector, connectivityManager) { networkRequest }
        assertThat(currentNetworkProvider.refreshNetworkStatus()).isEqualTo(wifi)
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
            CurrentNetworkProviderImpl(networkDetector, connectivityManager) { networkRequest }
        assertThat(networkProvider.refreshNetworkStatus()).isEqualTo(UNKNOWN_NETWORK)
    }

    @Test
    fun `listeners get the network re-detected when the permission grant check triggers`() {
        val networkDetector =
            mockk<NetworkDetector> {
                every { detectCurrentNetwork() } returns cellularWithoutSubtype andThen cellular
            }
        val currentNetworkProvider = createProvider(networkDetector, permissionGrantChecks = listOf(true))
        val notified = currentNetworkProvider.recordNetworkChanges()

        assertThat(currentNetworkProvider.currentNetwork).isEqualTo(cellular)
        assertThat(notified).containsExactly(cellular)
    }

    @Test
    fun `listeners are not notified when the network re-detected after the permission grant is unchanged`() {
        val networkDetector =
            mockk<NetworkDetector> {
                every { detectCurrentNetwork() } returns cellular
            }
        val currentNetworkProvider = createProvider(networkDetector, permissionGrantChecks = listOf(true))
        val notified = currentNetworkProvider.recordNetworkChanges()

        assertThat(currentNetworkProvider.currentNetwork).isEqualTo(cellular)
        assertThat(notified).isEmpty()
    }

    @Test
    fun `network is not re-detected while the permission grant check does not trigger`() {
        val networkDetector =
            mockk<NetworkDetector> {
                every { detectCurrentNetwork() } returns cellularWithoutSubtype andThen cellular
            }
        val currentNetworkProvider = createProvider(networkDetector, permissionGrantChecks = listOf(false))
        val notified = currentNetworkProvider.recordNetworkChanges()

        assertThat(currentNetworkProvider.currentNetwork).isEqualTo(cellularWithoutSubtype)
        assertThat(currentNetworkProvider.currentNetwork).isEqualTo(cellularWithoutSubtype)
        assertThat(notified).isEmpty()
        verify(exactly = 1) { networkDetector.detectCurrentNetwork() }
    }

    @Test
    fun `a failed re-detection after the permission grant keeps the previous network`() {
        val networkDetector =
            mockk<NetworkDetector> {
                every { detectCurrentNetwork() } returns cellularWithoutSubtype andThenThrows SecurityException("bug")
            }
        val currentNetworkProvider = createProvider(networkDetector, permissionGrantChecks = listOf(true))
        val notified = currentNetworkProvider.recordNetworkChanges()

        assertThat(currentNetworkProvider.currentNetwork).isEqualTo(cellularWithoutSubtype)
        assertThat(notified).isEmpty()
    }

    @Test
    fun `a network lost while re-detecting after the permission grant is kept`() {
        val networkDetector = mockk<NetworkDetector>()
        every { networkDetector.detectCurrentNetwork() } returns cellularWithoutSubtype andThenAnswer {
            // the network drops while the re-detection triggered by the grant is still running
            networkCallback.captured.onLost(fakeNet)
            cellular
        }
        val currentNetworkProvider = createProvider(networkDetector, permissionGrantChecks = listOf(true))
        val notified = currentNetworkProvider.recordNetworkChanges()

        assertThat(currentNetworkProvider.currentNetwork).isEqualTo(noNetwork)
        assertThat(notified).containsExactly(noNetwork)
    }

    @Test
    fun `a callback detection that started before the permission grant does not overwrite the network re-detected after it`() {
        lateinit var currentNetworkProvider: CurrentNetworkProvider
        val networkDetector = mockk<NetworkDetector>()
        every { networkDetector.detectCurrentNetwork() } returns cellularWithoutSubtype andThenAnswer {
            // a span reads the network after the grant while this callback detection is still running
            currentNetworkProvider.currentNetwork
            cellularWithoutSubtype
        } andThen cellular
        currentNetworkProvider = createProvider(networkDetector, permissionGrantChecks = listOf(true, false))
        val notified = currentNetworkProvider.recordNetworkChanges()

        networkCallback.captured.onAvailable(fakeNet)

        assertThat(currentNetworkProvider.currentNetwork).isEqualTo(cellular)
        assertThat(notified).containsExactly(cellular)
    }

    @Test
    fun `concurrent network observations do not fail or deadlock`() {
        val networkDetector =
            mockk<NetworkDetector> {
                every { detectCurrentNetwork() } returns wifi
            }
        val currentNetworkProvider = createProvider(networkDetector, permissionGrantChecks = listOf(true))
        val threads = 8
        val iterations = 1_000
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threads)

        // every thread goes through each path that stores a network, so the stores keep contending
        val tasks =
            (0 until threads).map { thread ->
                executor.submit {
                    start.await()
                    repeat(iterations) { iteration ->
                        when ((thread + iteration) % 4) {
                            0 -> networkCallback.captured.onAvailable(fakeNet)
                            1 -> networkCallback.captured.onLost(fakeNet)
                            2 -> currentNetworkProvider.refreshNetworkStatus()
                            else -> currentNetworkProvider.currentNetwork
                        }
                    }
                }
            }
        start.countDown()
        executor.shutdown()

        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue()
        tasks.forEach { it.get() } // rethrows a failure from any thread
        assertThat(currentNetworkProvider.currentNetwork).isIn(wifi, noNetwork)
    }

    /** [permissionGrantChecks] are the trigger's results in order; the last one repeats. */
    private fun createProvider(
        networkDetector: NetworkDetector,
        permissionGrantChecks: List<Boolean>,
    ): CurrentNetworkProvider =
        CurrentNetworkProviderImpl(
            networkDetector,
            mockk {
                every { registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
            },
            phoneStatePermissionGrantCheckTrigger =
                mockk {
                    every { consumeTrigger() } returnsMany permissionGrantChecks
                },
        )

    private fun CurrentNetworkProvider.recordNetworkChanges(): List<CurrentNetwork> =
        mutableListOf<CurrentNetwork>().also { notified ->
            addNetworkChangeListener { notified += it }
        }
}
