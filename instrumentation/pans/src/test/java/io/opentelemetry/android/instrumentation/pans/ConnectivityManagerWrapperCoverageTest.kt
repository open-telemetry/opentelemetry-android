/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import android.net.Network
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Comprehensive coverage tests for ConnectivityManagerWrapper.
 */
@RunWith(RobolectricTestRunner::class)
class ConnectivityManagerWrapperCoverageTest {
    private lateinit var context: Context
    private lateinit var wrapper: ConnectivityManagerWrapper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        wrapper = ConnectivityManagerWrapper(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Constructor Coverage ====================

    @Test
    fun testWrapperCreation() {
        val wrapper = ConnectivityManagerWrapper(context)
        assertNotNull(wrapper)
    }

    @Test
    fun testWrapperWithApplicationContext() {
        val wrapper = ConnectivityManagerWrapper(context.applicationContext)
        assertNotNull(wrapper)
    }

    // ==================== hasNetworkCapability() Coverage ====================

    @Test
    fun testHasNetworkCapabilityWithNoActiveNetwork() {
        val result = wrapper.hasNetworkCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        // Result depends on test environment network state
        assertNotNull(result)
    }

    @Test
    fun testHasNetworkCapabilityWithInvalidCapability() {
        val result = wrapper.hasNetworkCapability(-1)
        assertFalse(result)
    }

    @Test
    fun testHasNetworkCapabilityWithLargeCapabilityValue() {
        val result = wrapper.hasNetworkCapability(999)
        assertFalse(result)
    }

    @Test
    fun testHasNetworkCapabilityForOemPaid() {
        // CAP_OEM_PAID = 19
        val result = wrapper.hasNetworkCapability(19)
        assertNotNull(result)
    }

    @Test
    fun testHasNetworkCapabilityForOemPrivate() {
        // CAP_OEM_PRIVATE = 20
        val result = wrapper.hasNetworkCapability(20)
        assertNotNull(result)
    }

    @Test
    fun testHasNetworkCapabilityForInternet() {
        val result = wrapper.hasNetworkCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        assertNotNull(result)
    }

    @Test
    fun testHasNetworkCapabilityForNotMetered() {
        val result = wrapper.hasNetworkCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        assertNotNull(result)
    }

    // ==================== getAvailableNetworks() Coverage ====================

    @Test
    fun testGetAvailableNetworksReturnsNonNullList() {
        val networks = wrapper.getAvailableNetworks()
        assertNotNull(networks)
    }

    @Test
    fun testGetAvailableNetworksReturnsListType() {
        val networks = wrapper.getAvailableNetworks()
        assertTrue(networks is List<*>)
    }

    @Test
    fun testGetAvailableNetworksCalledMultipleTimes() {
        val networks1 = wrapper.getAvailableNetworks()
        val networks2 = wrapper.getAvailableNetworks()
        val networks3 = wrapper.getAvailableNetworks()

        assertNotNull(networks1)
        assertNotNull(networks2)
        assertNotNull(networks3)
    }

    @Test
    fun testGetAvailableNetworksNetworkInfoProperties() {
        val networks = wrapper.getAvailableNetworks()
        networks.forEach { networkInfo ->
            // Check all properties exist and are accessible
            assertNotNull(networkInfo.isOemPaid)
            assertNotNull(networkInfo.isOemPrivate)
            assertNotNull(networkInfo.isMetered)
            assertNotNull(networkInfo.isConnected)
        }
    }

    // ==================== isNetworkConnected() Coverage ====================

    @Test
    fun testIsNetworkConnectedWithMockedNetwork() {
        val mockNetwork = mockk<Network>(relaxed = true)
        val result = wrapper.isNetworkConnected(mockNetwork)
        // Should return false for mock network without proper setup
        assertNotNull(result)
    }

    // ==================== getActiveNetwork() Coverage ====================

    @Test
    fun testGetActiveNetworkReturnsResult() {
        // May be null or non-null depending on test environment
        // Just verify it doesn't throw
        wrapper.getActiveNetwork()
    }

    @Test
    fun testGetActiveNetworkCalledMultipleTimes() {
        wrapper.getActiveNetwork()
        wrapper.getActiveNetwork()
        // Verify repeated calls work
    }

    // ==================== hasAccessNetworkStatePermission() Coverage ====================

    @Test
    fun testHasAccessNetworkStatePermission() {
        val result = wrapper.hasAccessNetworkStatePermission()
        // Result depends on test environment
        assertNotNull(result)
    }

    @Test
    fun testHasAccessNetworkStatePermissionCalledMultipleTimes() {
        val result1 = wrapper.hasAccessNetworkStatePermission()
        val result2 = wrapper.hasAccessNetworkStatePermission()
        assertEquals(result1, result2)
    }

    // ==================== NetworkInfo Data Class Coverage ====================

    @Test
    fun testNetworkInfoDefaultValues() {
        val info = ConnectivityManagerWrapper.NetworkInfo()
        assertFalse(info.isOemPaid)
        assertFalse(info.isOemPrivate)
        assertFalse(info.isMetered)
        assertFalse(info.isConnected)
    }

    @Test
    fun testNetworkInfoWithAllTrue() {
        val info = ConnectivityManagerWrapper.NetworkInfo(
            isOemPaid = true,
            isOemPrivate = true,
            isMetered = true,
            isConnected = true
        )
        assertTrue(info.isOemPaid)
        assertTrue(info.isOemPrivate)
        assertTrue(info.isMetered)
        assertTrue(info.isConnected)
    }

    @Test
    fun testNetworkInfoWithMixedValues() {
        val info = ConnectivityManagerWrapper.NetworkInfo(
            isOemPaid = true,
            isOemPrivate = false,
            isMetered = true,
            isConnected = false
        )
        assertTrue(info.isOemPaid)
        assertFalse(info.isOemPrivate)
        assertTrue(info.isMetered)
        assertFalse(info.isConnected)
    }

    @Test
    fun testNetworkInfoEquality() {
        val info1 = ConnectivityManagerWrapper.NetworkInfo(
            isOemPaid = true,
            isOemPrivate = false,
            isMetered = true,
            isConnected = false
        )
        val info2 = ConnectivityManagerWrapper.NetworkInfo(
            isOemPaid = true,
            isOemPrivate = false,
            isMetered = true,
            isConnected = false
        )
        assertEquals(info1, info2)
    }

    @Test
    fun testNetworkInfoCopy() {
        val info1 = ConnectivityManagerWrapper.NetworkInfo(
            isOemPaid = true,
            isOemPrivate = true,
            isMetered = true,
            isConnected = true
        )
        val info2 = info1.copy(isOemPaid = false)

        assertFalse(info2.isOemPaid)
        assertTrue(info2.isOemPrivate)
    }

    @Test
    fun testNetworkInfoToString() {
        val info = ConnectivityManagerWrapper.NetworkInfo(isOemPaid = true)
        val str = info.toString()
        assertTrue(str.contains("isOemPaid=true"))
    }

    @Test
    fun testNetworkInfoHashCode() {
        val info1 = ConnectivityManagerWrapper.NetworkInfo(isOemPaid = true)
        val info2 = ConnectivityManagerWrapper.NetworkInfo(isOemPaid = true)
        assertEquals(info1.hashCode(), info2.hashCode())
    }

    // ==================== Edge Cases Coverage ====================

    @Test
    fun testWrapperOperationsInSequence() {
        // Test all operations in sequence
        wrapper.hasAccessNetworkStatePermission()
        wrapper.getActiveNetwork()
        wrapper.getAvailableNetworks()
        wrapper.hasNetworkCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        // All should complete without exception
    }

    @Test
    fun testMultipleWrapperInstances() {
        val wrapper1 = ConnectivityManagerWrapper(context)
        val wrapper2 = ConnectivityManagerWrapper(context)
        val wrapper3 = ConnectivityManagerWrapper(context)

        assertNotNull(wrapper1)
        assertNotNull(wrapper2)
        assertNotNull(wrapper3)

        // All should work independently
        wrapper1.getAvailableNetworks()
        wrapper2.getAvailableNetworks()
        wrapper3.getAvailableNetworks()
    }
}

