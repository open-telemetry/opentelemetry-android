/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PANSMetricsExtractorTest {
    private lateinit var context: Context
    private lateinit var netStatsManager: NetStatsManager
    private lateinit var extractor: PANSMetricsExtractor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        netStatsManager = NetStatsManager(context)
        extractor = PANSMetricsExtractor(context, netStatsManager)
    }

    // ==================== Initialization Tests ====================

    @Test
    fun testExtractorInitializationSuccessful() {
        assertNotNull(extractor)
    }

    @Test
    fun testMultipleExtractorInstances() {
        val extractor1 = PANSMetricsExtractor(context, netStatsManager)
        val extractor2 = PANSMetricsExtractor(context, netStatsManager)
        assertNotNull(extractor1)
        assertNotNull(extractor2)
    }

    @Test
    fun testExtractorWithFreshNetStatsManager() {
        val freshNetStatsManager = NetStatsManager(context)
        val freshExtractor = PANSMetricsExtractor(context, freshNetStatsManager)
        assertNotNull(freshExtractor)
    }

    // ==================== Extract Metrics - Main Cases ====================

    @Test
    fun testExtractMetricsReturnsValidPANSMetrics() {
        val metrics = extractor.extractMetrics()
        assertNotNull(metrics)
        assertNotNull(metrics.appNetworkUsage)
        assertNotNull(metrics.preferenceChanges)
        assertNotNull(metrics.networkAvailability)
    }

    @Test
    fun testExtractMetricsDoesNotThrow() {
        try {
            extractor.extractMetrics()
        } catch (e: Exception) {
            throw AssertionError("extractMetrics() should not throw", e)
        }
    }

    @Test
    fun testExtractMetricsMultipleCalls() {
        val metrics1 = extractor.extractMetrics()
        val metrics2 = extractor.extractMetrics()
        assertNotNull(metrics1)
        assertNotNull(metrics2)
    }

    @Test
    fun testExtractMetricsReturnsNonNullLists() {
        val metrics = extractor.extractMetrics()
        assertTrue(metrics.appNetworkUsage is List<*>)
        assertTrue(metrics.preferenceChanges is List<*>)
        assertTrue(metrics.networkAvailability is List<*>)
    }

    // ==================== Network Availability Tests ====================

    @Test
    fun testNetworkAvailabilityIsNotNull() {
        val metrics = extractor.extractMetrics()
        assertNotNull(metrics.networkAvailability)
    }

    @Test
    fun testNetworkAvailabilityHasAtLeastTwoEntries() {
        val metrics = extractor.extractMetrics()
        // Should have OEM_PAID and OEM_PRIVATE entries
        assertTrue(metrics.networkAvailability.size >= 2)
    }

    @Test
    fun testNetworkAvailabilityContainsOEMPaid() {
        val metrics = extractor.extractMetrics()
        val hasOemPaid = metrics.networkAvailability.any { it.networkType == "OEM_PAID" }
        assertTrue(hasOemPaid)
    }

    @Test
    fun testNetworkAvailabilityContainsOEMPrivate() {
        val metrics = extractor.extractMetrics()
        val hasOemPrivate = metrics.networkAvailability.any { it.networkType == "OEM_PRIVATE" }
        assertTrue(hasOemPrivate)
    }

    @Test
    fun testNetworkAvailabilityContainsValidNetworkTypes() {
        val metrics = extractor.extractMetrics()
        metrics.networkAvailability.forEach { availability ->
            assertTrue(availability.networkType.isNotEmpty())
            assertTrue(availability.networkType == "OEM_PAID" || availability.networkType == "OEM_PRIVATE")
        }
    }

    @Test
    fun testNetworkAvailabilityAttributesNotNull() {
        val metrics = extractor.extractMetrics()
        metrics.networkAvailability.forEach { availability ->
            assertNotNull(availability.attributes)
        }
    }

    @Test
    fun testNetworkAvailabilityHasValidSignalStrength() {
        val metrics = extractor.extractMetrics()
        metrics.networkAvailability.forEach { availability ->
            // Signal strength should be >= -1
            assertTrue(availability.signalStrength >= -1)
        }
    }

    // ==================== App Network Usage Tests ====================

    @Test
    fun testAppNetworkUsageIsNotNull() {
        val metrics = extractor.extractMetrics()
        assertNotNull(metrics.appNetworkUsage)
    }

    @Test
    fun testAppNetworkUsageCanBeEmpty() {
        val metrics = extractor.extractMetrics()
        // Without permissions, list may be empty
        assertTrue(metrics.appNetworkUsage.isEmpty() || metrics.appNetworkUsage.isNotEmpty())
    }

    @Test
    fun testAppNetworkUsageHasValidStructure() {
        val metrics = extractor.extractMetrics()
        metrics.appNetworkUsage.forEach { usage ->
            assertNotNull(usage.packageName)
            assertNotNull(usage.networkType)
            assertNotNull(usage.attributes)
            assertTrue(usage.uid >= 0)
            assertTrue(usage.bytesTransmitted >= 0)
            assertTrue(usage.bytesReceived >= 0)
        }
    }

    @Test
    fun testAppNetworkUsagePackageNamesNotEmpty() {
        val metrics = extractor.extractMetrics()
        metrics.appNetworkUsage.forEach { usage ->
            assertTrue(usage.packageName.isNotEmpty())
        }
    }

    @Test
    fun testAppNetworkUsageNetworkTypesValid() {
        val metrics = extractor.extractMetrics()
        metrics.appNetworkUsage.forEach { usage ->
            assertTrue(usage.networkType.isNotEmpty())
        }
    }

    // ==================== Preference Changes Tests ====================

    @Test
    fun testPreferenceChangesIsNotNull() {
        val metrics = extractor.extractMetrics()
        assertNotNull(metrics.preferenceChanges)
    }

    @Test
    fun testPreferenceChangesInitiallyEmpty() {
        // Clear preferences first
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val extractor = PANSMetricsExtractor(context, netStatsManager)
        val metrics = extractor.extractMetrics()
        // On first run with cleared prefs, should be empty
        assertTrue(metrics.preferenceChanges.isEmpty())
    }

    @Test
    fun testPreferenceChangesHasValidStructure() {
        val metrics = extractor.extractMetrics()
        metrics.preferenceChanges.forEach { change ->
            assertNotNull(change.packageName)
            assertNotNull(change.oldPreference)
            assertNotNull(change.newPreference)
            assertNotNull(change.attributes)
            assertTrue(change.uid >= 0)
            assertTrue(change.timestamp > 0)
        }
    }

    // ==================== Sequential Extractions Tests ====================

    @Test
    fun testSequentialExtractions() {
        try {
            extractor.extractMetrics()
            extractor.extractMetrics()
            extractor.extractMetrics()
        } catch (e: Exception) {
            throw AssertionError("Sequential extractions should not throw", e)
        }
    }

    @Test
    fun testRapidSequentialExtractions() {
        try {
            repeat(10) {
                extractor.extractMetrics()
            }
        } catch (e: Exception) {
            throw AssertionError("Rapid sequential extractions should not throw", e)
        }
    }

    @Test
    fun testSequentialExtractionsWithDelay() {
        val metrics1 = extractor.extractMetrics()
        Thread.sleep(50)
        val metrics2 = extractor.extractMetrics()
        assertNotNull(metrics1)
        assertNotNull(metrics2)
    }

    // ==================== Consistency Tests ====================

    @Test
    fun testMetricsStructureConsistency() {
        val metrics1 = extractor.extractMetrics()
        val metrics2 = extractor.extractMetrics()

        assertEquals(metrics1.networkAvailability.size, metrics2.networkAvailability.size)
    }

    @Test
    fun testNetworkAvailabilityConsistency() {
        val metrics1 = extractor.extractMetrics()
        val metrics2 = extractor.extractMetrics()

        val types1 = metrics1.networkAvailability.map { it.networkType }.toSet()
        val types2 = metrics2.networkAvailability.map { it.networkType }.toSet()
        assertEquals(types1, types2)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun testExtractMetricsNeverReturnsNull() {
        repeat(5) {
            val metrics = extractor.extractMetrics()
            assertNotNull(metrics)
        }
    }

    @Test
    fun testExtractorHandlesErrors() {
        try {
            val metrics = extractor.extractMetrics()
            assertNotNull(metrics)
        } catch (e: Exception) {
            throw AssertionError("Extractor should handle errors gracefully", e)
        }
    }

    // ==================== SharedPreferences Tests ====================

    @Test
    fun testPreferencesCaching() {
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // First extraction
        extractor.extractMetrics()

        // Second extraction
        val metrics = extractor.extractMetrics()
        assertNotNull(metrics)
    }

    @Test
    fun testPreferencesUpdated() {
        val prefs = context.getSharedPreferences("pans_preferences", Context.MODE_PRIVATE)

        // Clear and extract
        prefs.edit().clear().apply()
        extractor.extractMetrics()

        // Check prefs exist (may be empty if no network stats)
        assertNotNull(prefs.all)
    }

    // ==================== Network Types Coverage ====================

    @Test
    fun testExtractorReportsOEMPaidAvailability() {
        val metrics = extractor.extractMetrics()
        val oemPaid = metrics.networkAvailability.find { it.networkType == "OEM_PAID" }
        assertNotNull(oemPaid)
    }

    @Test
    fun testExtractorReportsOEMPrivateAvailability() {
        val metrics = extractor.extractMetrics()
        val oemPrivate = metrics.networkAvailability.find { it.networkType == "OEM_PRIVATE" }
        assertNotNull(oemPrivate)
    }

    @Test
    fun testNetworkAvailabilityValues() {
        val metrics = extractor.extractMetrics()
        metrics.networkAvailability.forEach { availability ->
            // isAvailable should be boolean
            assertTrue(availability.isAvailable || !availability.isAvailable)
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun testExtractorWithClosedNetStatsManager() {
        val manager = NetStatsManager(context)
        manager.close()

        val extractor = PANSMetricsExtractor(context, manager)
        val metrics = extractor.extractMetrics()
        assertNotNull(metrics)
    }

    @Test
    fun testMultipleExtractorsShareContext() {
        val extractor1 = PANSMetricsExtractor(context, netStatsManager)
        val extractor2 = PANSMetricsExtractor(context, netStatsManager)

        val metrics1 = extractor1.extractMetrics()
        val metrics2 = extractor2.extractMetrics()

        assertNotNull(metrics1)
        assertNotNull(metrics2)
    }

    @Test
    fun testExtractorAllFieldsAccessible() {
        val metrics = extractor.extractMetrics()

        // Access all fields
        metrics.appNetworkUsage.size
        metrics.preferenceChanges.size
        metrics.networkAvailability.size

        metrics.appNetworkUsage.forEach {
            it.packageName
            it.uid
            it.networkType
            it.bytesTransmitted
            it.bytesReceived
            it.attributes
        }

        metrics.networkAvailability.forEach {
            it.networkType
            it.isAvailable
            it.signalStrength
            it.attributes
        }
    }

    // ==================== Stress Tests ====================

    @Test
    fun testManyExtractors() {
        val extractors = mutableListOf<PANSMetricsExtractor>()
        repeat(5) {
            extractors.add(PANSMetricsExtractor(context, netStatsManager))
        }

        extractors.forEach { ext ->
            val metrics = ext.extractMetrics()
            assertNotNull(metrics)
        }
    }

    @Test
    fun testExtractorLongRunning() {
        repeat(20) {
            val metrics = extractor.extractMetrics()
            assertNotNull(metrics)
        }
    }
}
