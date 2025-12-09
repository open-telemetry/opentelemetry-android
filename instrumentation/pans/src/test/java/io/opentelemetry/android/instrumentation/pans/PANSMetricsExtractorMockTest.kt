/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
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
 * Tests for PANSMetricsExtractor with mocking to cover exception paths and edge cases.
 */
@RunWith(RobolectricTestRunner::class)
class PANSMetricsExtractorMockTest {
    private lateinit var context: Context
    private lateinit var mockNetStatsManager: NetStatsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockNetStatsManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Network Stats with Data Tests ====================

    @Test
    fun testExtractMetricsWithNetworkStats() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app1",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis()
            ),
            NetStatsManager.AppNetworkStats(
                uid = 1001,
                packageName = "com.test.app2",
                networkType = "OEM_PRIVATE",
                rxBytes = 2000L,
                txBytes = 1000L,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertNotNull(metrics)
        assertEquals(2, metrics.appNetworkUsage.size)
        assertEquals("com.test.app1", metrics.appNetworkUsage[0].packageName)
        assertEquals(1000, metrics.appNetworkUsage[0].uid)
        assertEquals("OEM_PAID", metrics.appNetworkUsage[0].networkType)
        assertEquals(500L, metrics.appNetworkUsage[0].bytesTransmitted)
        assertEquals(1000L, metrics.appNetworkUsage[0].bytesReceived)
    }

    @Test
    fun testExtractMetricsWithSingleNetworkStat() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.single.app",
                networkType = "OEM_PAID",
                rxBytes = 500L,
                txBytes = 250L,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(1, metrics.appNetworkUsage.size)
        assertEquals("com.single.app", metrics.appNetworkUsage[0].packageName)
    }

    @Test
    fun testExtractMetricsWithLargeNetworkStats() {
        val stats = (1..100).map { i ->
            NetStatsManager.AppNetworkStats(
                uid = 1000 + i,
                packageName = "com.test.app$i",
                networkType = if (i % 2 == 0) "OEM_PAID" else "OEM_PRIVATE",
                rxBytes = i * 1000L,
                txBytes = i * 500L,
                timestamp = System.currentTimeMillis()
            )
        }
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(100, metrics.appNetworkUsage.size)
    }

    // ==================== Exception Handling Tests ====================

    @Test
    fun testExtractMetricsHandlesNetworkStatsException() {
        every { mockNetStatsManager.getNetworkStats() } throws RuntimeException("Test exception")

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        // Should return empty metrics rather than throwing
        assertNotNull(metrics)
        assertTrue(metrics.appNetworkUsage.isEmpty())
    }

    @Test
    fun testExtractMetricsHandlesSecurityException() {
        every { mockNetStatsManager.getNetworkStats() } throws SecurityException("Permission denied")

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertNotNull(metrics)
        assertTrue(metrics.appNetworkUsage.isEmpty())
    }

    @Test
    fun testExtractMetricsHandlesNullPointerException() {
        every { mockNetStatsManager.getNetworkStats() } throws NullPointerException("Null value")

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertNotNull(metrics)
    }

    // ==================== Preference Changes Tests with Mock Data ====================

    @Test
    fun testDetectPreferenceChangesWithNetworkTypeChange() {
        // Set up initial preferences
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        prefs.edit().putString("1000:com.test.app", "OEM_PAID").apply()

        // Return stats with different network type
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PRIVATE", // Changed from OEM_PAID
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertNotNull(metrics)
        // Should detect preference change
        assertEquals(1, metrics.preferenceChanges.size)
        assertEquals("OEM_PAID", metrics.preferenceChanges[0].oldPreference)
        assertEquals("OEM_PRIVATE", metrics.preferenceChanges[0].newPreference)
    }

    @Test
    fun testNoPreferenceChangeWhenNetworkTypeSame() {
        // Set up initial preferences
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        prefs.edit().putString("1000:com.test.app", "OEM_PAID").apply()

        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID", // Same as stored
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertTrue(metrics.preferenceChanges.isEmpty())
    }

    @Test
    fun testPreferenceChangesMultipleApps() {
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        prefs.edit()
            .putString("1000:com.app1", "OEM_PAID")
            .putString("1001:com.app2", "OEM_PRIVATE")
            .apply()

        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.app1",
                networkType = "OEM_PRIVATE", // Changed
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis()
            ),
            NetStatsManager.AppNetworkStats(
                uid = 1001,
                packageName = "com.app2",
                networkType = "OEM_PAID", // Changed
                rxBytes = 2000L,
                txBytes = 1000L,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(2, metrics.preferenceChanges.size)
    }

    // ==================== Network Availability Tests ====================

    @Test
    fun testNetworkAvailabilityOEMNetworksNotAvailable() {
        every { mockNetStatsManager.getNetworkStats() } returns emptyList()

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        // Should report both OEM networks as unavailable
        assertTrue(metrics.networkAvailability.isNotEmpty())
        val oemPaid = metrics.networkAvailability.find { it.networkType == "OEM_PAID" }
        val oemPrivate = metrics.networkAvailability.find { it.networkType == "OEM_PRIVATE" }
        assertNotNull(oemPaid)
        assertNotNull(oemPrivate)
    }

    @Test
    fun testNetworkAvailabilityAttributes() {
        every { mockNetStatsManager.getNetworkStats() } returns emptyList()

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        metrics.networkAvailability.forEach { availability ->
            assertNotNull(availability.attributes)
            val networkTypeAttr = availability.attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("network_type"))
            assertEquals(availability.networkType, networkTypeAttr)
        }
    }

    // ==================== Stress and Edge Cases ====================

    @Test
    fun testExtractMetricsEmptyStats() {
        every { mockNetStatsManager.getNetworkStats() } returns emptyList()

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertTrue(metrics.appNetworkUsage.isEmpty())
    }

    @Test
    fun testExtractMetricsWithZeroBytesStats() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.zero.app",
                networkType = "OEM_PAID",
                rxBytes = 0L,
                txBytes = 0L,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(1, metrics.appNetworkUsage.size)
        assertEquals(0L, metrics.appNetworkUsage[0].bytesTransmitted)
        assertEquals(0L, metrics.appNetworkUsage[0].bytesReceived)
    }

    @Test
    fun testExtractMetricsWithMaxLongBytes() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.max.app",
                networkType = "OEM_PAID",
                rxBytes = Long.MAX_VALUE,
                txBytes = Long.MAX_VALUE,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(Long.MAX_VALUE, metrics.appNetworkUsage[0].bytesTransmitted)
        assertEquals(Long.MAX_VALUE, metrics.appNetworkUsage[0].bytesReceived)
    }

    @Test
    fun testMultipleExtractionsCachePreferences() {
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)

        // First extraction - no changes expected
        val metrics1 = extractor.extractMetrics()
        assertTrue(metrics1.preferenceChanges.isEmpty())

        // Second extraction - still no changes as network type is same
        val metrics2 = extractor.extractMetrics()
        assertTrue(metrics2.preferenceChanges.isEmpty())
    }

    @Test
    fun testSequentialExtractionsWithChangingStats() {
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)

        // First extraction with OEM_PAID
        val stats1 = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats1
        extractor.extractMetrics()

        // Second extraction with OEM_PRIVATE - should detect change
        val stats2 = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PRIVATE",
                rxBytes = 2000L,
                txBytes = 1000L,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats2
        val metrics2 = extractor.extractMetrics()

        assertEquals(1, metrics2.preferenceChanges.size)
    }

    // ==================== App Network Usage Attributes Tests ====================

    @Test
    fun testAppNetworkUsageAttributesComplete() {
        val timestamp = System.currentTimeMillis()
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = timestamp
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        val usage = metrics.appNetworkUsage[0]
        assertNotNull(usage.attributes)

        val packageAttr = usage.attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("app_package_name"))
        val networkTypeAttr = usage.attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("network_type"))
        val uidAttr = usage.attributes.get(io.opentelemetry.api.common.AttributeKey.longKey("uid"))
        val timestampAttr = usage.attributes.get(io.opentelemetry.api.common.AttributeKey.longKey("timestamp_ms"))

        assertEquals("com.test.app", packageAttr)
        assertEquals("OEM_PAID", networkTypeAttr)
        assertEquals(1000L, uidAttr)
        assertEquals(timestamp, timestampAttr)
    }

    @Test
    fun testPreferenceChangeAttributesComplete() {
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        prefs.edit().putString("1000:com.test.app", "OEM_PAID").apply()

        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000,
                packageName = "com.test.app",
                networkType = "OEM_PRIVATE",
                rxBytes = 1000L,
                txBytes = 500L,
                timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        val change = metrics.preferenceChanges[0]
        assertNotNull(change.attributes)

        val packageAttr = change.attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("app_package_name"))
        val oldPrefAttr = change.attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("old_preference"))
        val newPrefAttr = change.attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("new_preference"))
        val uidAttr = change.attributes.get(io.opentelemetry.api.common.AttributeKey.longKey("uid"))

        assertEquals("com.test.app", packageAttr)
        assertEquals("OEM_PAID", oldPrefAttr)
        assertEquals("OEM_PRIVATE", newPrefAttr)
        assertEquals(1000L, uidAttr)
    }
}

