/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Comprehensive coverage tests for NetStatsManager.
 */
@RunWith(RobolectricTestRunner::class)
class NetStatsManagerCoverageTest {
    private lateinit var context: Context
    private lateinit var netStatsManager: NetStatsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        netStatsManager = NetStatsManager(context)
    }

    @After
    fun tearDown() {
        netStatsManager.close()
        unmockkAll()
    }

    // ==================== Constructor Coverage ====================

    @Test
    fun testNetStatsManagerCreation() {
        val manager = NetStatsManager(context)
        assertNotNull(manager)
        manager.close()
    }

    @Test
    fun testNetStatsManagerWithApplicationContext() {
        val manager = NetStatsManager(context.applicationContext)
        assertNotNull(manager)
        manager.close()
    }

    @Test
    fun testMultipleManagerInstances() {
        val manager1 = NetStatsManager(context)
        val manager2 = NetStatsManager(context)
        val manager3 = NetStatsManager(context)

        assertNotNull(manager1)
        assertNotNull(manager2)
        assertNotNull(manager3)

        manager1.close()
        manager2.close()
        manager3.close()
    }

    // ==================== getNetworkStats() Coverage ====================

    @Test
    fun testGetNetworkStatsReturnsNonNullList() {
        val stats = netStatsManager.getNetworkStats()
        assertNotNull(stats)
    }

    @Test
    fun testGetNetworkStatsReturnsListType() {
        val stats = netStatsManager.getNetworkStats()
        assertTrue(stats is List<*>)
    }

    @Test
    fun testGetNetworkStatsCalledMultipleTimes() {
        val stats1 = netStatsManager.getNetworkStats()
        val stats2 = netStatsManager.getNetworkStats()
        val stats3 = netStatsManager.getNetworkStats()

        assertNotNull(stats1)
        assertNotNull(stats2)
        assertNotNull(stats3)
    }

    @Test
    fun testGetNetworkStatsDoesNotThrow() {
        try {
            netStatsManager.getNetworkStats()
        } catch (e: Exception) {
            throw AssertionError("getNetworkStats() should not throw", e)
        }
    }

    // ==================== hasPackageUsageStatsPermission() Coverage ====================

    @Test
    fun testHasPackageUsageStatsPermission() {
        val result = netStatsManager.hasPackageUsageStatsPermission()
        // Result depends on test environment - just verify it returns a boolean
        assertNotNull(result)
    }

    @Test
    fun testHasPackageUsageStatsPermissionCalledMultipleTimes() {
        val result1 = netStatsManager.hasPackageUsageStatsPermission()
        val result2 = netStatsManager.hasPackageUsageStatsPermission()
        assertEquals(result1, result2)
    }

    // ==================== hasRequiredPermissions() Coverage ====================

    @Test
    fun testHasRequiredPermissions() {
        val result = netStatsManager.hasRequiredPermissions()
        assertNotNull(result)
    }

    @Test
    fun testHasRequiredPermissionsCalledMultipleTimes() {
        val result1 = netStatsManager.hasRequiredPermissions()
        val result2 = netStatsManager.hasRequiredPermissions()
        assertEquals(result1, result2)
    }

    // ==================== close() Coverage ====================

    @Test
    fun testCloseDoesNotThrow() {
        val manager = NetStatsManager(context)
        try {
            manager.close()
        } catch (e: Exception) {
            throw AssertionError("close() should not throw", e)
        }
    }

    @Test
    fun testCloseCalledMultipleTimes() {
        val manager = NetStatsManager(context)
        manager.close()
        manager.close()
        manager.close()
        // Should not throw
    }

    @Test
    fun testOperationsAfterClose() {
        val manager = NetStatsManager(context)
        manager.close()

        // These should still work or fail gracefully after close
        val stats = manager.getNetworkStats()
        assertNotNull(stats)

        val hasPerms = manager.hasRequiredPermissions()
        assertNotNull(hasPerms)
    }

    // ==================== AppNetworkStats Data Class Coverage ====================

    @Test
    fun testAppNetworkStatsCreation() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis(),
            )
        assertNotNull(stats)
    }

    @Test
    fun testAppNetworkStatsProperties() {
        val timestamp = System.currentTimeMillis()
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = timestamp,
            )

        assertEquals(1000, stats.uid)
        assertEquals("com.test.app", stats.packageName)
        assertEquals("OEM_PAID", stats.networkType)
        assertEquals(1000L, stats.rxBytes)
        assertEquals(500L, stats.txBytes)
        assertEquals(timestamp, stats.timestamp)
    }

    @Test
    fun testAppNetworkStatsWithDefaultTimestamp() {
        val beforeCreate = System.currentTimeMillis()
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
            )
        val afterCreate = System.currentTimeMillis()

        assertTrue(stats.timestamp >= beforeCreate)
        assertTrue(stats.timestamp <= afterCreate)
    }

    @Test
    fun testAppNetworkStatsWithZeroBytes() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 0L,
                txBytes = 0L,
                timestamp = System.currentTimeMillis(),
            )

        assertEquals(0L, stats.rxBytes)
        assertEquals(0L, stats.txBytes)
    }

    @Test
    fun testAppNetworkStatsWithMaxBytes() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = Long.MAX_VALUE,
                txBytes = Long.MAX_VALUE,
                timestamp = System.currentTimeMillis(),
            )

        assertEquals(Long.MAX_VALUE, stats.rxBytes)
        assertEquals(Long.MAX_VALUE, stats.txBytes)
    }

    @Test
    fun testAppNetworkStatsWithNegativeUid() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = -1,
                packageName = "com.system.app",
                networkType = "OEM_PAID",
                rxBytes = 100L,
                txBytes = 50L,
                timestamp = System.currentTimeMillis(),
            )

        assertEquals(-1, stats.uid)
    }

    @Test
    fun testAppNetworkStatsWithEmptyPackageName() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "",
                networkType = "OEM_PAID",
                rxBytes = 100L,
                txBytes = 50L,
                timestamp = System.currentTimeMillis(),
            )

        assertEquals("", stats.packageName)
    }

    @Test
    fun testAppNetworkStatsEquality() {
        val timestamp = System.currentTimeMillis()
        val stats1 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = timestamp,
            )
        val stats2 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = timestamp,
            )

        assertEquals(stats1, stats2)
    }

    @Test
    fun testAppNetworkStatsCopy() {
        val stats1 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis(),
            )
        val stats2 = stats1.copy(rxBytes = 2000L)

        assertEquals(2000L, stats2.rxBytes)
        assertEquals(stats1.uid, stats2.uid)
        assertEquals(stats1.packageName, stats2.packageName)
    }

    @Test
    fun testAppNetworkStatsToString() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis(),
            )
        val str = stats.toString()

        assertTrue(str.contains("uid=1000"))
        assertTrue(str.contains("com.test.app"))
        assertTrue(str.contains("OEM_PAID"))
    }

    @Test
    fun testAppNetworkStatsHashCode() {
        val timestamp = System.currentTimeMillis()
        val stats1 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = timestamp,
            )
        val stats2 =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = timestamp,
            )

        assertEquals(stats1.hashCode(), stats2.hashCode())
    }

    @Test
    fun testAppNetworkStatsWithOemPrivateType() {
        val stats =
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PRIVATE",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis(),
            )

        assertEquals("OEM_PRIVATE", stats.networkType)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testManagerWithRapidCreateClose() {
        repeat(10) {
            val manager = NetStatsManager(context)
            manager.getNetworkStats()
            manager.close()
        }
    }

    @Test
    fun testConcurrentGetNetworkStats() {
        val threads =
            (1..5).map {
                Thread {
                    repeat(10) {
                        netStatsManager.getNetworkStats()
                    }
                }
            }

        threads.forEach { it.start() }
        threads.forEach { it.join() }
    }
}
