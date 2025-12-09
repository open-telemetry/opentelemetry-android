/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for ConnectivityManagerWrapper with mocking to cover exception paths and OEM network scenarios.
 */
@RunWith(RobolectricTestRunner::class)
class ConnectivityManagerWrapperMockTest {
    private lateinit var mockContext: Context
    private lateinit var mockConnectivityManager: ConnectivityManager
    private lateinit var mockNetwork: Network
    private lateinit var mockNetworkCapabilities: NetworkCapabilities

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockConnectivityManager = mockk(relaxed = true)
        mockNetwork = mockk(relaxed = true)
        mockNetworkCapabilities = mockk(relaxed = true)

        every { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== OEM Network Capability Tests ====================

    @Test
    fun testHasOEMPaidNetworkCapability() {
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(19) } returns true // CAP_OEM_PAID

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val hasCapability = wrapper.hasNetworkCapability(19)

        assertTrue(hasCapability)
    }

    @Test
    fun testHasOEMPrivateNetworkCapability() {
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(20) } returns true // CAP_OEM_PRIVATE

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val hasCapability = wrapper.hasNetworkCapability(20)

        assertTrue(hasCapability)
    }

    @Test
    fun testNoOEMNetworkCapability() {
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(any()) } returns false

        val wrapper = ConnectivityManagerWrapper(mockContext)

        assertFalse(wrapper.hasNetworkCapability(19))
        assertFalse(wrapper.hasNetworkCapability(20))
    }

    // ==================== Exception Handling Tests ====================

    @Test
    fun testHasNetworkCapabilityHandlesNullActiveNetwork() {
        every { mockConnectivityManager.activeNetwork } returns null

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val hasCapability = wrapper.hasNetworkCapability(19)

        assertFalse(hasCapability)
    }

    @Test
    fun testHasNetworkCapabilityHandlesNullCapabilities() {
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns null

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val hasCapability = wrapper.hasNetworkCapability(19)

        assertFalse(hasCapability)
    }

    @Test
    fun testHasNetworkCapabilityHandlesException() {
        every { mockConnectivityManager.activeNetwork } throws RuntimeException("Test exception")

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val hasCapability = wrapper.hasNetworkCapability(19)

        assertFalse(hasCapability)
    }

    @Test
    fun testHasNetworkCapabilityHandlesSecurityException() {
        every { mockConnectivityManager.activeNetwork } throws SecurityException("Permission denied")

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val hasCapability = wrapper.hasNetworkCapability(19)

        assertFalse(hasCapability)
    }

    // ==================== Get Available Networks Tests ====================

    @Test
    fun testGetAvailableNetworksWithOEMNetworks() {
        val network1 = mockk<Network>()
        val network2 = mockk<Network>()
        val caps1 = mockk<NetworkCapabilities>()
        val caps2 = mockk<NetworkCapabilities>()

        every { mockConnectivityManager.allNetworks } returns arrayOf(network1, network2)
        every { mockConnectivityManager.getNetworkCapabilities(network1) } returns caps1
        every { mockConnectivityManager.getNetworkCapabilities(network2) } returns caps2
        every { caps1.hasCapability(19) } returns true // OEM_PAID
        every { caps1.hasCapability(20) } returns false
        every { caps1.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns false
        every { caps1.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { caps2.hasCapability(19) } returns false
        every { caps2.hasCapability(20) } returns true // OEM_PRIVATE
        every { caps2.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns true
        every { caps2.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val networks = wrapper.getAvailableNetworks()

        assertEquals(2, networks.size)
        assertTrue(networks[0].isOemPaid)
        assertFalse(networks[0].isOemPrivate)
        assertTrue(networks[0].isMetered)
        assertFalse(networks[1].isOemPaid)
        assertTrue(networks[1].isOemPrivate)
        assertFalse(networks[1].isMetered)
    }

    @Test
    fun testGetAvailableNetworksHandlesEmptyArray() {
        every { mockConnectivityManager.allNetworks } returns emptyArray()

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val networks = wrapper.getAvailableNetworks()

        assertTrue(networks.isEmpty())
    }

    @Test
    fun testGetAvailableNetworksHandlesNullCapabilities() {
        every { mockConnectivityManager.allNetworks } returns arrayOf(mockNetwork)
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns null

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val networks = wrapper.getAvailableNetworks()

        assertTrue(networks.isEmpty())
    }

    @Test
    fun testGetAvailableNetworksHandlesException() {
        every { mockConnectivityManager.allNetworks } throws RuntimeException("Test exception")

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val networks = wrapper.getAvailableNetworks()

        assertTrue(networks.isEmpty())
    }

    @Test
    fun testGetAvailableNetworksHandlesCapabilityException() {
        every { mockConnectivityManager.allNetworks } returns arrayOf(mockNetwork)
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } throws RuntimeException("Test")

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val networks = wrapper.getAvailableNetworks()

        assertTrue(networks.isEmpty())
    }

    // ==================== Is Network Connected Tests ====================

    @Test
    fun testIsNetworkConnectedTrue() {
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val isConnected = wrapper.isNetworkConnected(mockNetwork)

        assertTrue(isConnected)
    }

    @Test
    fun testIsNetworkConnectedFalse() {
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val isConnected = wrapper.isNetworkConnected(mockNetwork)

        assertFalse(isConnected)
    }

    @Test
    fun testIsNetworkConnectedWithNullCapabilities() {
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns null

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val isConnected = wrapper.isNetworkConnected(mockNetwork)

        assertFalse(isConnected)
    }

    @Test
    fun testIsNetworkConnectedHandlesException() {
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } throws RuntimeException("Test")

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val isConnected = wrapper.isNetworkConnected(mockNetwork)

        assertFalse(isConnected)
    }

    // ==================== Get Active Network Tests ====================

    @Test
    fun testGetActiveNetworkReturnsNetwork() {
        every { mockConnectivityManager.activeNetwork } returns mockNetwork

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val network = wrapper.getActiveNetwork()

        assertNotNull(network)
        assertEquals(mockNetwork, network)
    }

    @Test
    fun testGetActiveNetworkReturnsNull() {
        every { mockConnectivityManager.activeNetwork } returns null

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val network = wrapper.getActiveNetwork()

        assertNull(network)
    }

    @Test
    fun testGetActiveNetworkHandlesException() {
        every { mockConnectivityManager.activeNetwork } throws RuntimeException("Test exception")

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val network = wrapper.getActiveNetwork()

        assertNull(network)
    }

    // ==================== Null ConnectivityManager Tests ====================

    @Test
    fun testWrapperWithNullConnectivityManager() {
        every { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns null

        val wrapper = ConnectivityManagerWrapper(mockContext)

        assertFalse(wrapper.hasNetworkCapability(19))
        assertTrue(wrapper.getAvailableNetworks().isEmpty())
        assertNull(wrapper.getActiveNetwork())
    }

    // ==================== Multiple Networks Scenarios ====================

    @Test
    fun testGetAvailableNetworksWithMixedConnectivity() {
        val network1 = mockk<Network>()
        val network2 = mockk<Network>()
        val network3 = mockk<Network>()
        val caps1 = mockk<NetworkCapabilities>()
        val caps2 = mockk<NetworkCapabilities>()
        val caps3 = mockk<NetworkCapabilities>()

        every { mockConnectivityManager.allNetworks } returns arrayOf(network1, network2, network3)
        every { mockConnectivityManager.getNetworkCapabilities(network1) } returns caps1
        every { mockConnectivityManager.getNetworkCapabilities(network2) } returns caps2
        every { mockConnectivityManager.getNetworkCapabilities(network3) } returns caps3

        // Network 1: OEM_PAID, metered, connected
        every { caps1.hasCapability(19) } returns true
        every { caps1.hasCapability(20) } returns false
        every { caps1.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns false
        every { caps1.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        // Network 2: OEM_PRIVATE, not metered, connected
        every { caps2.hasCapability(19) } returns false
        every { caps2.hasCapability(20) } returns true
        every { caps2.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns true
        every { caps2.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        // Network 3: Both OEM types, metered, not connected
        every { caps3.hasCapability(19) } returns true
        every { caps3.hasCapability(20) } returns true
        every { caps3.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns false
        every { caps3.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

        val wrapper = ConnectivityManagerWrapper(mockContext)
        val networks = wrapper.getAvailableNetworks()

        assertEquals(3, networks.size)

        // Verify network 1
        assertTrue(networks[0].isOemPaid)
        assertFalse(networks[0].isOemPrivate)
        assertTrue(networks[0].isMetered)
        assertTrue(networks[0].isConnected)

        // Verify network 2
        assertFalse(networks[1].isOemPaid)
        assertTrue(networks[1].isOemPrivate)
        assertFalse(networks[1].isMetered)
        assertTrue(networks[1].isConnected)

        // Verify network 3
        assertTrue(networks[2].isOemPaid)
        assertTrue(networks[2].isOemPrivate)
        assertTrue(networks[2].isMetered)
        assertFalse(networks[2].isConnected)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testAllCapabilityConstants() {
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(any()) } returns false

        val wrapper = ConnectivityManagerWrapper(mockContext)

        // Test various capability values
        listOf(0, 1, 2, 3, 12, 18, 19, 20, 21, -1, 100, Int.MAX_VALUE).forEach { cap ->
            assertFalse(wrapper.hasNetworkCapability(cap))
        }
    }

    @Test
    fun testRapidCapabilityChecks() {
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(any()) } returns true

        val wrapper = ConnectivityManagerWrapper(mockContext)

        repeat(100) {
            assertTrue(wrapper.hasNetworkCapability(19))
            assertTrue(wrapper.hasNetworkCapability(20))
        }
    }
}

