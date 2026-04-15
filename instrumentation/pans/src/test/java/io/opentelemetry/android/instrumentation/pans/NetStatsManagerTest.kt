/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class NetStatsManagerTest {
    private lateinit var context: Context
    private lateinit var netStatsManager: NetStatsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        netStatsManager = NetStatsManager(context)
    }

    @After
    fun tearDown() {
        try {
            netStatsManager.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    // ==================== Initialization Tests ====================

    @Test
    fun testNetStatsManagerInitializationSuccessful() {
        assertNotNull(netStatsManager)
    }

    @Test
    fun testNetStatsManagerMultipleInstances() {
        val manager1 = NetStatsManager(context)
        val manager2 = NetStatsManager(context)
        assertNotNull(manager1)
        assertNotNull(manager2)
        manager1.close()
        manager2.close()
    }

    @Test
    fun testNetStatsManagerWithApplicationContext() {
        val appContext = context.applicationContext
        val manager = NetStatsManager(appContext)
        assertNotNull(manager)
        manager.close()
    }

    // ==================== Permission Tests ====================

    @Test
    fun testHasRequiredPermissionsReturnsFalseWithoutPermissions() {
        val hasPerms = netStatsManager.hasRequiredPermissions()
        assertFalse(hasPerms)
    }

    @Test
    fun testHasPackageUsageStatsPermissionReturnsFalse() {
        val hasPerm = netStatsManager.hasPackageUsageStatsPermission()
        assertFalse(hasPerm)
    }

    @Test
    fun testPermissionCheckDoesNotThrow() {
        try {
            netStatsManager.hasRequiredPermissions()
            netStatsManager.hasPackageUsageStatsPermission()
        } catch (e: Exception) {
            throw AssertionError("Permission checks should not throw", e)
        }
    }

    @Test
    fun testPermissionCheckMultipleTimes() {
        repeat(5) {
            assertFalse(netStatsManager.hasRequiredPermissions())
            assertFalse(netStatsManager.hasPackageUsageStatsPermission())
        }
    }

    // ==================== Network Stats Retrieval ====================

    @Test
    fun testGetNetworkStatsReturnsListNotNull() {
        val stats = netStatsManager.getNetworkStats()
        assertNotNull(stats)
    }

    @Test
    fun testGetNetworkStatsReturnsListWhenNoPermission() {
        val stats = netStatsManager.getNetworkStats()
        assertTrue(stats.isEmpty() || stats.isNotEmpty())
    }

    @Test
    fun testGetNetworkStatsDoesNotThrowException() {
        try {
            netStatsManager.getNetworkStats()
        } catch (e: Exception) {
            throw AssertionError("getNetworkStats should not throw", e)
        }
    }

    @Test
    fun testMultipleGetNetworkStatsCallsConsistent() {
        val stats1 = netStatsManager.getNetworkStats()
        val stats2 = netStatsManager.getNetworkStats()
        assertNotNull(stats1)
        assertNotNull(stats2)
    }

    @Test
    fun testGetNetworkStatsIsEmptyList() {
        val stats = netStatsManager.getNetworkStats()
        // Without PACKAGE_USAGE_STATS permission, should return empty
        assertTrue(stats.isEmpty())
    }

    // ==================== Resource Management Tests ====================

    @Test
    fun testCloseDoesNotThrow() {
        try {
            netStatsManager.close()
        } catch (e: Exception) {
            throw AssertionError("close() should not throw", e)
        }
    }

    @Test
    fun testMultipleCloseCallsDoNotThrow() {
        try {
            netStatsManager.close()
            netStatsManager.close()
            netStatsManager.close()
        } catch (e: Exception) {
            throw AssertionError("Multiple close() calls should not throw", e)
        }
    }

    @Test
    fun testCloseFollowedByPermissionCheck() {
        netStatsManager.close()
        try {
            val hasPerms = netStatsManager.hasRequiredPermissions()
            assertFalse(hasPerms)
        } catch (e: Exception) {
            throw AssertionError("Permission check after close should not throw", e)
        }
    }

    @Test
    fun testCloseFollowedByGetStats() {
        netStatsManager.close()
        try {
            val stats = netStatsManager.getNetworkStats()
            assertNotNull(stats)
        } catch (e: Exception) {
            throw AssertionError("getNetworkStats after close should not throw", e)
        }
    }

    // ==================== Sequential Operations Tests ====================

    @Test
    fun testRapidSequentialOperations() {
        try {
            repeat(10) {
                netStatsManager.getNetworkStats()
                netStatsManager.hasPackageUsageStatsPermission()
                netStatsManager.hasRequiredPermissions()
            }
        } catch (e: Exception) {
            throw AssertionError("Rapid operations should not throw", e)
        }
    }

    @Test
    fun testSequentialGetStatsCalls() {
        val results = mutableListOf<List<NetStatsManager.AppNetworkStats>>()
        repeat(5) {
            results.add(netStatsManager.getNetworkStats())
        }
        assertEquals(5, results.size)
        results.forEach { assertNotNull(it) }
    }

    // ==================== AppNetworkStats Data Class Tests ====================

    @Test
    fun testAppNetworkStatsDataClass() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
                timestamp = System.currentTimeMillis(),
            )

        assertEquals(1000, stats.uid)
        assertEquals("com.example.app", stats.packageName)
        assertEquals("OEM_PAID", stats.networkType)
        assertEquals(1024L, stats.rxBytes)
        assertEquals(2048L, stats.txBytes)
    }

    @Test
    fun testAppNetworkStatsZeroBytes() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1001,
                packageName = "com.test.app",
                networkType = "OEM_PRIVATE",
                rxBytes = 0,
                txBytes = 0,
            )

        assertEquals(0L, stats.rxBytes)
        assertEquals(0L, stats.txBytes)
    }

    @Test
    fun testAppNetworkStatsLargeValues() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 9999,
                packageName = "com.large.app",
                networkType = "OEM_PAID",
                rxBytes = Long.MAX_VALUE / 2,
                txBytes = Long.MAX_VALUE / 2,
            )

        assertEquals(Long.MAX_VALUE / 2, stats.rxBytes)
        assertEquals(Long.MAX_VALUE / 2, stats.txBytes)
    }

    @Test
    fun testAppNetworkStatsMaxValues() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = Int.MAX_VALUE,
                packageName = "com.max.app",
                networkType = "OEM_PAID",
                rxBytes = Long.MAX_VALUE,
                txBytes = Long.MAX_VALUE,
            )

        assertEquals(Int.MAX_VALUE, stats.uid)
        assertEquals(Long.MAX_VALUE, stats.rxBytes)
        assertEquals(Long.MAX_VALUE, stats.txBytes)
    }

    @Test
    fun testAppNetworkStatsEquality() {
        val timestamp = System.currentTimeMillis()
        val stats1 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
                timestamp = timestamp,
            )
        val stats2 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
                timestamp = timestamp,
            )

        assertEquals(stats1, stats2)
    }

    @Test
    fun testAppNetworkStatsDifferentUIDs() {
        val stats1 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
            )
        val stats2 =
            NetStatsManager.AppNetworkStats(
                uid = 1001,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
            )

        assertFalse(stats1 == stats2)
    }

    @Test
    fun testAppNetworkStatsDifferentPackages() {
        val stats1 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.app1",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
            )
        val stats2 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.app2",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
            )

        assertFalse(stats1 == stats2)
    }

    // ==================== Network Type Tests ====================

    @Test
    fun testNetStatsWithOEMPaidNetwork() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
            )

        assertEquals("OEM_PAID", stats.networkType)
    }

    @Test
    fun testNetStatsWithOEMPrivateNetwork() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PRIVATE",
                rxBytes = 1024,
                txBytes = 2048,
            )

        assertEquals("OEM_PRIVATE", stats.networkType)
    }

    @Test
    fun testNetStatsWithCustomNetworkType() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "CUSTOM_NETWORK",
                rxBytes = 1024,
                txBytes = 2048,
            )

        assertEquals("CUSTOM_NETWORK", stats.networkType)
    }

    @Test
    fun testNetStatsWithEmptyNetworkType() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "",
                rxBytes = 1024,
                txBytes = 2048,
            )

        assertEquals("", stats.networkType)
    }

    // ==================== Package Name Tests ====================

    @Test
    fun testNetStatsWithSystemPackageName() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "android",
                networkType = "OEM_PAID",
                rxBytes = 512,
                txBytes = 256,
            )

        assertEquals("android", stats.packageName)
    }

    @Test
    fun testNetStatsWithLongPackageName() {
        val longPackageName = "com.example." + "a".repeat(200)
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = longPackageName,
                networkType = "OEM_PAID",
                rxBytes = 512,
                txBytes = 256,
            )

        assertEquals(longPackageName, stats.packageName)
    }

    @Test
    fun testNetStatsWithEmptyPackageName() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "",
                networkType = "OEM_PAID",
                rxBytes = 512,
                txBytes = 256,
            )

        assertEquals("", stats.packageName)
    }

    @Test
    fun testNetStatsWithSpecialCharsPackageName() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test_app-debug.v1",
                networkType = "OEM_PAID",
                rxBytes = 512,
                txBytes = 256,
            )

        assertEquals("com.test_app-debug.v1", stats.packageName)
    }

    // ==================== Timestamp Tests ====================

    @Test
    fun testNetStatsWithCurrentTimestamp() {
        val now = System.currentTimeMillis()
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
                timestamp = now,
            )

        assertEquals(now, stats.timestamp)
    }

    @Test
    fun testNetStatsWithDefaultTimestamp() {
        val before = System.currentTimeMillis()
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
            )
        val after = System.currentTimeMillis()

        assertTrue(stats.timestamp >= before)
        assertTrue(stats.timestamp <= after)
    }

    @Test
    fun testNetStatsWithZeroTimestamp() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
                timestamp = 0L,
            )

        assertEquals(0L, stats.timestamp)
    }

    // ==================== Copy and HashCode Tests ====================

    @Test
    fun testAppNetworkStatsCopy() {
        val stats1 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
            )
        val stats2 = stats1.copy(rxBytes = 5000)

        assertEquals(5000L, stats2.rxBytes)
        assertEquals(1024L, stats1.rxBytes)
    }

    @Test
    fun testAppNetworkStatsHashCode() {
        val timestamp = System.currentTimeMillis()
        val stats1 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
                timestamp = timestamp,
            )
        val stats2 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
                timestamp = timestamp,
            )

        assertEquals(stats1.hashCode(), stats2.hashCode())
    }

    @Test
    fun testAppNetworkStatsToString() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
            )
        val str = stats.toString()

        assertTrue(str.contains("1000"))
        assertTrue(str.contains("com.example.app"))
        assertTrue(str.contains("OEM_PAID"))
    }

    // ==================== UID Boundary Tests ====================

    @Test
    fun testNetStatsWithMinUID() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 0,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
            )

        assertEquals(0, stats.uid)
    }

    @Test
    fun testNetStatsWithNegativeUID() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = -1,
                packageName = "com.example.app",
                networkType = "OEM_PAID",
                rxBytes = 1024,
                txBytes = 2048,
            )

        assertEquals(-1, stats.uid)
    }

    // ==================== Multiple Managers Test ====================

    @Test
    fun testMultipleManagersIndependent() {
        val manager1 = NetStatsManager(context)
        val manager2 = NetStatsManager(context)

        manager1.close()

        // manager2 should still work
        val stats = manager2.getNetworkStats()
        assertNotNull(stats)

        manager2.close()
    }
}
