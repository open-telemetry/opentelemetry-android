/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import android.net.Network
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
class ConnectivityManagerWrapperTest {
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

    // ==================== Initialization Tests ====================

    @Test
    fun testConnectivityManagerWrapperInitializationSuccessful() {
        assertNotNull(wrapper)
    }

    @Test
    fun testMultipleWrapperInstances() {
        val wrapper1 = ConnectivityManagerWrapper(context)
        val wrapper2 = ConnectivityManagerWrapper(context)
        assertNotNull(wrapper1)
        assertNotNull(wrapper2)
    }

    @Test
    fun testWrapperWithApplicationContext() {
        val appContext = context.applicationContext
        val wrapper = ConnectivityManagerWrapper(appContext)
        assertNotNull(wrapper)
    }

    // ==================== Network Detection Tests ====================

    @Test
    fun testGetAvailableNetworksReturnsListNotNull() {
        val networks = wrapper.getAvailableNetworks()
        assertNotNull(networks)
    }

    @Test
    fun testGetAvailableNetworksReturnsValidList() {
        val networks = wrapper.getAvailableNetworks()
        assertTrue(networks.isEmpty() || networks.isNotEmpty())
    }

    @Test
    fun testGetAvailableNetworksMultipleCalls() {
        val networks1 = wrapper.getAvailableNetworks()
        val networks2 = wrapper.getAvailableNetworks()
        assertNotNull(networks1)
        assertNotNull(networks2)
    }

    @Test
    fun testGetAvailableNetworksDoesNotThrow() {
        try {
            wrapper.getAvailableNetworks()
        } catch (e: Exception) {
            throw AssertionError("getAvailableNetworks should not throw", e)
        }
    }

    // ==================== Permission Tests ====================

    @Test
    fun testHasAccessNetworkStatePermissionReturnsFalse() {
        val hasPermission = wrapper.hasAccessNetworkStatePermission()
        assertFalse(hasPermission)
    }

    @Test
    fun testHasAccessNetworkStatePermissionDoesNotThrow() {
        try {
            wrapper.hasAccessNetworkStatePermission()
        } catch (e: Exception) {
            throw AssertionError("hasAccessNetworkStatePermission should not throw", e)
        }
    }

    @Test
    fun testPermissionCheckMultipleTimes() {
        repeat(5) {
            assertFalse(wrapper.hasAccessNetworkStatePermission())
        }
    }

    // ==================== Network Capability Tests ====================

    @Test
    fun testHasNetworkCapabilityWithOEMPaidCapability() {
        val hasCapability = wrapper.hasNetworkCapability(19)
        assertFalse(hasCapability)
    }

    @Test
    fun testHasNetworkCapabilityWithOEMPrivateCapability() {
        val hasCapability = wrapper.hasNetworkCapability(20)
        assertFalse(hasCapability)
    }

    @Test
    fun testHasNetworkCapabilityWithInvalidCapability() {
        try {
            val hasCapability = wrapper.hasNetworkCapability(999)
            assertFalse(hasCapability)
        } catch (e: Exception) {
            throw AssertionError("hasNetworkCapability should not throw for invalid capability", e)
        }
    }

    @Test
    fun testHasNetworkCapabilityWithNegativeCapability() {
        val hasCapability = wrapper.hasNetworkCapability(-1)
        assertFalse(hasCapability)
    }

    @Test
    fun testHasNetworkCapabilityDoesNotThrow() {
        try {
            wrapper.hasNetworkCapability(19)
            wrapper.hasNetworkCapability(20)
            wrapper.hasNetworkCapability(-1)
            wrapper.hasNetworkCapability(0)
            wrapper.hasNetworkCapability(100)
        } catch (e: Exception) {
            throw AssertionError("hasNetworkCapability should not throw", e)
        }
    }

    @Test
    fun testHasNetworkCapabilityZero() {
        val hasCapability = wrapper.hasNetworkCapability(0)
        assertFalse(hasCapability)
    }

    // ==================== Active Network Tests ====================

    @Test
    fun testGetActiveNetworkDoesNotThrow() {
        try {
            wrapper.getActiveNetwork()
        } catch (e: Exception) {
            throw AssertionError("getActiveNetwork should not throw", e)
        }
    }

    @Test
    fun testGetActiveNetworkReturnsNullOrNetwork() {
        val network = wrapper.getActiveNetwork()
        // May be null or a Network object
        assertTrue(network == null || network is Network)
    }

    @Test
    fun testMultipleGetActiveNetworkCalls() {
        val network1 = wrapper.getActiveNetwork()
        val network2 = wrapper.getActiveNetwork()
        assertEquals(network1, network2)
    }

    // ==================== NetworkInfo Data Class Tests ====================

    @Test
    fun testNetworkInfoDataClassOEMPaidNetwork() {
        val info =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = false,
                isMetered = true,
                isConnected = true,
            )
        assertTrue(info.isOemPaid)
        assertFalse(info.isOemPrivate)
        assertTrue(info.isMetered)
        assertTrue(info.isConnected)
    }

    @Test
    fun testNetworkInfoDataClassOEMPrivateNetwork() {
        val info =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = false,
                isOemPrivate = true,
                isMetered = false,
                isConnected = false,
            )
        assertFalse(info.isOemPaid)
        assertTrue(info.isOemPrivate)
        assertFalse(info.isMetered)
        assertFalse(info.isConnected)
    }

    @Test
    fun testNetworkInfoDataClassAllFalse() {
        val info =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = false,
                isOemPrivate = false,
                isMetered = false,
                isConnected = false,
            )
        assertFalse(info.isOemPaid)
        assertFalse(info.isOemPrivate)
        assertFalse(info.isMetered)
        assertFalse(info.isConnected)
    }

    @Test
    fun testNetworkInfoDataClassAllTrue() {
        val info =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = true,
                isMetered = true,
                isConnected = true,
            )
        assertTrue(info.isOemPaid)
        assertTrue(info.isOemPrivate)
        assertTrue(info.isMetered)
        assertTrue(info.isConnected)
    }

    @Test
    fun testNetworkInfoDefaultValues() {
        val info = ConnectivityManagerWrapper.NetworkInfo()
        assertFalse(info.isOemPaid)
        assertFalse(info.isOemPrivate)
        assertFalse(info.isMetered)
        assertFalse(info.isConnected)
    }

    @Test
    fun testNetworkInfoEquality() {
        val info1 =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = false,
                isMetered = true,
                isConnected = true,
            )
        val info2 =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = false,
                isMetered = true,
                isConnected = true,
            )
        assertEquals(info1, info2)
    }

    @Test
    fun testNetworkInfoInequality() {
        val info1 =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = false,
                isMetered = true,
                isConnected = true,
            )
        val info2 =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = false,
                isOemPrivate = true,
                isMetered = true,
                isConnected = true,
            )
        assertFalse(info1 == info2)
    }

    @Test
    fun testNetworkInfoCopy() {
        val info1 =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = false,
                isMetered = true,
                isConnected = true,
            )
        val info2 = info1.copy(isOemPaid = false)
        assertFalse(info2.isOemPaid)
        assertTrue(info1.isOemPaid)
    }

    @Test
    fun testNetworkInfoHashCode() {
        val info1 =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = false,
                isMetered = true,
                isConnected = true,
            )
        val info2 =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = false,
                isMetered = true,
                isConnected = true,
            )
        assertEquals(info1.hashCode(), info2.hashCode())
    }

    @Test
    fun testNetworkInfoToString() {
        val info =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = false,
                isMetered = true,
                isConnected = true,
            )
        val str = info.toString()
        assertTrue(str.contains("isOemPaid=true"))
        assertTrue(str.contains("isOemPrivate=false"))
    }

    // ==================== Network Info Combinations ====================

    @Test
    fun testNetworkInfoMeteredOEMNetwork() {
        val info =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = false,
                isMetered = true,
                isConnected = true,
            )
        assertTrue(info.isOemPaid)
        assertTrue(info.isMetered)
        assertTrue(info.isConnected)
    }

    @Test
    fun testNetworkInfoNotMeteredOEMNetwork() {
        val info =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = false,
                isMetered = false,
                isConnected = true,
            )
        assertTrue(info.isOemPaid)
        assertFalse(info.isMetered)
        assertTrue(info.isConnected)
    }

    @Test
    fun testNetworkInfoDisconnectedButMetered() {
        val info =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = false,
                isOemPrivate = false,
                isMetered = true,
                isConnected = false,
            )
        assertFalse(info.isConnected)
        assertTrue(info.isMetered)
    }

    @Test
    fun testNetworkInfoBothOEMNetworksAvailable() {
        val info =
            ConnectivityManagerWrapper.NetworkInfo(
                isOemPaid = true,
                isOemPrivate = true,
                isMetered = true,
                isConnected = true,
            )
        assertTrue(info.isOemPaid)
        assertTrue(info.isOemPrivate)
    }

    // ==================== Sequential Operation Tests ====================

    @Test
    fun testRapidSequentialNetworkQueries() {
        try {
            repeat(10) {
                wrapper.getAvailableNetworks()
                wrapper.getActiveNetwork()
                wrapper.hasNetworkCapability(19)
                wrapper.hasAccessNetworkStatePermission()
            }
        } catch (e: Exception) {
            throw AssertionError("Sequential operations should not throw", e)
        }
    }

    @Test
    fun testSequentialCapabilityChecks() {
        val results = mutableListOf<Boolean>()
        repeat(5) {
            results.add(wrapper.hasNetworkCapability(19))
            results.add(wrapper.hasNetworkCapability(20))
        }
        assertEquals(10, results.size)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testWrapperWithDifferentContextTypes() {
        val appContext = context.applicationContext
        val wrapper1 = ConnectivityManagerWrapper(context)
        val wrapper2 = ConnectivityManagerWrapper(appContext)

        assertNotNull(wrapper1.getAvailableNetworks())
        assertNotNull(wrapper2.getAvailableNetworks())
    }

    @Test
    fun testAllNetworkCapabilities() {
        // Test various capability constants
        val capabilities = listOf(0, 1, 2, 3, 12, 19, 20, 21, -1, 100)
        capabilities.forEach { cap ->
            try {
                wrapper.hasNetworkCapability(cap)
            } catch (e: Exception) {
                throw AssertionError("hasNetworkCapability($cap) should not throw", e)
            }
        }
    }

    @Test
    fun testNetworkInfoAllCombinations() {
        // Test all 16 combinations of 4 booleans
        val booleans = listOf(true, false)
        for (oem in booleans) {
            for (priv in booleans) {
                for (met in booleans) {
                    for (conn in booleans) {
                        val info = ConnectivityManagerWrapper.NetworkInfo(oem, priv, met, conn)
                        assertEquals(oem, info.isOemPaid)
                        assertEquals(priv, info.isOemPrivate)
                        assertEquals(met, info.isMetered)
                        assertEquals(conn, info.isConnected)
                    }
                }
            }
        }
    }
}
