/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
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
 * Comprehensive coverage tests for PANSMetricsExtractor.
 * Focuses on edge cases, exception handling, and all code paths.
 */
@RunWith(RobolectricTestRunner::class)
class PANSMetricsExtractorCoverageTest {
    private lateinit var context: Context
    private lateinit var mockNetStatsManager: NetStatsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockNetStatsManager = mockk(relaxed = true)

        // Clear any saved preferences
        context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== extractMetrics() coverage ====================

    @Test
    fun testExtractMetricsWithEmptyNetworkStats() {
        every { mockNetStatsManager.getNetworkStats() } returns emptyList()

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertNotNull(metrics)
        assertTrue(metrics.appNetworkUsage.isEmpty())
    }

    @Test
    fun testExtractMetricsWithMultipleApps() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.app1", networkType = "OEM_PAID",
                rxBytes = 1000L, txBytes = 500L, timestamp = System.currentTimeMillis()
            ),
            NetStatsManager.AppNetworkStats(
                uid = 1001, packageName = "com.app2", networkType = "OEM_PRIVATE",
                rxBytes = 2000L, txBytes = 1000L, timestamp = System.currentTimeMillis()
            ),
            NetStatsManager.AppNetworkStats(
                uid = 1002, packageName = "com.app3", networkType = "OEM_PAID",
                rxBytes = 3000L, txBytes = 1500L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(3, metrics.appNetworkUsage.size)
    }

    @Test
    fun testExtractMetricsHandlesGeneralException() {
        every { mockNetStatsManager.getNetworkStats() } throws Exception("Test exception")

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertNotNull(metrics)
        assertTrue(metrics.appNetworkUsage.isEmpty())
    }

    @Test
    fun testExtractMetricsHandlesOutOfMemoryError() {
        every { mockNetStatsManager.getNetworkStats() } throws OutOfMemoryError("OOM")

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)

        // Should handle gracefully or rethrow - depends on implementation
        try {
            val metrics = extractor.extractMetrics()
            assertNotNull(metrics)
        } catch (_: OutOfMemoryError) {
            // Also acceptable - OOM should propagate
        }
    }

    // ==================== extractAppNetworkUsage() coverage ====================

    @Test
    fun testAppNetworkUsageWithZeroBytes() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.zero.app", networkType = "OEM_PAID",
                rxBytes = 0L, txBytes = 0L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(1, metrics.appNetworkUsage.size)
        assertEquals(0L, metrics.appNetworkUsage[0].bytesReceived)
        assertEquals(0L, metrics.appNetworkUsage[0].bytesTransmitted)
    }

    @Test
    fun testAppNetworkUsageWithMaxLongBytes() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.max.app", networkType = "OEM_PAID",
                rxBytes = Long.MAX_VALUE, txBytes = Long.MAX_VALUE, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(Long.MAX_VALUE, metrics.appNetworkUsage[0].bytesReceived)
        assertEquals(Long.MAX_VALUE, metrics.appNetworkUsage[0].bytesTransmitted)
    }

    @Test
    fun testAppNetworkUsageWithNegativeUid() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = -1, packageName = "com.system.app", networkType = "OEM_PAID",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(-1, metrics.appNetworkUsage[0].uid)
    }

    @Test
    fun testAppNetworkUsageWithEmptyPackageName() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "", networkType = "OEM_PAID",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals("", metrics.appNetworkUsage[0].packageName)
    }

    @Test
    fun testAppNetworkUsageWithSpecialCharactersInPackageName() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.test.app_with-special.chars123", networkType = "OEM_PAID",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals("com.test.app_with-special.chars123", metrics.appNetworkUsage[0].packageName)
    }

    @Test
    fun testAppNetworkUsageAttributesContainRequiredFields() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.test.app", networkType = "OEM_PAID",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        val attributes = metrics.appNetworkUsage[0].attributes
        assertNotNull(attributes)
        assertNotNull(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("app_package_name")))
        assertNotNull(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("network_type")))
    }

    // ==================== detectPreferenceChanges() coverage ====================

    @Test
    fun testPreferenceChangeDetection() {
        // Set up initial preference
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString("1000:com.test.app", "OEM_PAID").apply()

        // Return stats with changed network type
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.test.app", networkType = "OEM_PRIVATE",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(1, metrics.preferenceChanges.size)
        assertEquals("OEM_PAID", metrics.preferenceChanges[0].oldPreference)
        assertEquals("OEM_PRIVATE", metrics.preferenceChanges[0].newPreference)
    }

    @Test
    fun testNoPreferenceChangeForNewApp() {
        // Clear preferences - no existing entry
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.new.app", networkType = "OEM_PAID",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        // No change because no previous preference
        assertEquals(0, metrics.preferenceChanges.size)
    }

    @Test
    fun testNoPreferenceChangeWhenSameNetworkType() {
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString("1000:com.test.app", "OEM_PAID").apply()

        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.test.app", networkType = "OEM_PAID",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(0, metrics.preferenceChanges.size)
    }

    @Test
    fun testMultiplePreferenceChanges() {
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("1000:com.app1", "OEM_PAID")
            .putString("1001:com.app2", "OEM_PRIVATE")
            .apply()

        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.app1", networkType = "OEM_PRIVATE",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            ),
            NetStatsManager.AppNetworkStats(
                uid = 1001, packageName = "com.app2", networkType = "OEM_PAID",
                rxBytes = 200L, txBytes = 100L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        assertEquals(2, metrics.preferenceChanges.size)
    }

    @Test
    fun testPreferenceChangeAttributesAreValid() {
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString("1000:com.test.app", "OEM_PAID").apply()

        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.test.app", networkType = "OEM_PRIVATE",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        val change = metrics.preferenceChanges[0]
        assertNotNull(change.attributes)
        assertEquals("com.test.app", change.packageName)
        assertEquals(1000, change.uid)
        assertTrue(change.timestamp > 0)
    }

    // ==================== extractNetworkAvailability() coverage ====================

    @Test
    fun testNetworkAvailabilityWhenNoOemNetworksDetected() {
        every { mockNetStatsManager.getNetworkStats() } returns emptyList()

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)
        val metrics = extractor.extractMetrics()

        // Should have at least 2 entries (OEM_PAID and OEM_PRIVATE as unavailable)
        assertTrue(metrics.networkAvailability.size >= 2)

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
            assertTrue(availability.networkType.isNotEmpty())
        }
    }

    // ==================== Repeated extraction tests ====================

    @Test
    fun testRepeatedExtraction() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.test.app", networkType = "OEM_PAID",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)

        // First extraction
        val metrics1 = extractor.extractMetrics()
        assertNotNull(metrics1)

        // Second extraction
        val metrics2 = extractor.extractMetrics()
        assertNotNull(metrics2)

        // Third extraction
        val metrics3 = extractor.extractMetrics()
        assertNotNull(metrics3)
    }

    @Test
    fun testExtractionAfterPreferenceSaved() {
        val stats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.test.app", networkType = "OEM_PAID",
                rxBytes = 100L, txBytes = 50L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns stats

        val extractor = PANSMetricsExtractor(context, mockNetStatsManager)

        // First extraction - saves preferences
        val metrics1 = extractor.extractMetrics()
        assertEquals(0, metrics1.preferenceChanges.size)

        // Change network type for second extraction
        val changedStats = listOf(
            NetStatsManager.AppNetworkStats(
                uid = 1000, packageName = "com.test.app", networkType = "OEM_PRIVATE",
                rxBytes = 200L, txBytes = 100L, timestamp = System.currentTimeMillis()
            )
        )
        every { mockNetStatsManager.getNetworkStats() } returns changedStats

        // Second extraction - should detect change
        val metrics2 = extractor.extractMetrics()
        assertEquals(1, metrics2.preferenceChanges.size)
    }
}

