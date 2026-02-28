/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Comprehensive coverage tests for PANS data classes and helper functions.
 */
@RunWith(RobolectricTestRunner::class)
class PANSMetricsCoverageTest {
    // ==================== PANSMetrics Data Class ====================

    @Test
    fun testPANSMetricsDefaultValues() {
        val metrics = PANSMetrics()
        assertTrue(metrics.appNetworkUsage.isEmpty())
        assertTrue(metrics.preferenceChanges.isEmpty())
        assertTrue(metrics.networkAvailability.isEmpty())
    }

    @Test
    fun testPANSMetricsWithAllLists() {
        val usage =
            listOf(
                AppNetworkUsage(
                    packageName = "com.test",
                    uid = 1000,
                    networkType = "OEM_PAID",
                    bytesTransmitted = 100L,
                    bytesReceived = 200L,
                    attributes = Attributes.empty(),
                ),
            )
        val changes =
            listOf(
                PreferenceChange(
                    packageName = "com.test",
                    uid = 1000,
                    oldPreference = "OEM_PAID",
                    newPreference = "OEM_PRIVATE",
                    timestamp = System.currentTimeMillis(),
                    attributes = Attributes.empty(),
                ),
            )
        val availability =
            listOf(
                NetworkAvailability(
                    networkType = "OEM_PAID",
                    isAvailable = true,
                    attributes = Attributes.empty(),
                ),
            )

        val metrics =
            PANSMetrics(
                appNetworkUsage = usage,
                preferenceChanges = changes,
                networkAvailability = availability,
            )

        assertEquals(1, metrics.appNetworkUsage.size)
        assertEquals(1, metrics.preferenceChanges.size)
        assertEquals(1, metrics.networkAvailability.size)
    }

    @Test
    fun testPANSMetricsEquality() {
        val metrics1 = PANSMetrics()
        val metrics2 = PANSMetrics()
        assertEquals(metrics1, metrics2)
    }

    @Test
    fun testPANSMetricsCopy() {
        val metrics1 = PANSMetrics()
        val usage =
            listOf(
                AppNetworkUsage(
                    packageName = "com.test",
                    uid = 1000,
                    networkType = "OEM_PAID",
                    bytesTransmitted = 100L,
                    bytesReceived = 200L,
                    attributes = Attributes.empty(),
                ),
            )
        val metrics2 = metrics1.copy(appNetworkUsage = usage)

        assertEquals(1, metrics2.appNetworkUsage.size)
        assertTrue(metrics1.appNetworkUsage.isEmpty())
    }

    // ==================== AppNetworkUsage Data Class ====================

    @Test
    fun testAppNetworkUsageCreation() {
        val usage =
            AppNetworkUsage(
                packageName = "com.test.app",
                uid = 1000,
                networkType = "OEM_PAID",
                bytesTransmitted = 500L,
                bytesReceived = 1000L,
                attributes = Attributes.empty(),
            )

        assertEquals("com.test.app", usage.packageName)
        assertEquals(1000, usage.uid)
        assertEquals("OEM_PAID", usage.networkType)
        assertEquals(500L, usage.bytesTransmitted)
        assertEquals(1000L, usage.bytesReceived)
        assertNotNull(usage.attributes)
    }

    @Test
    fun testAppNetworkUsageWithZeroBytes() {
        val usage =
            AppNetworkUsage(
                packageName = "com.test",
                uid = 1000,
                networkType = "OEM_PAID",
                bytesTransmitted = 0L,
                bytesReceived = 0L,
                attributes = Attributes.empty(),
            )

        assertEquals(0L, usage.bytesTransmitted)
        assertEquals(0L, usage.bytesReceived)
    }

    @Test
    fun testAppNetworkUsageWithMaxBytes() {
        val usage =
            AppNetworkUsage(
                packageName = "com.test",
                uid = 1000,
                networkType = "OEM_PAID",
                bytesTransmitted = Long.MAX_VALUE,
                bytesReceived = Long.MAX_VALUE,
                attributes = Attributes.empty(),
            )

        assertEquals(Long.MAX_VALUE, usage.bytesTransmitted)
        assertEquals(Long.MAX_VALUE, usage.bytesReceived)
    }

    @Test
    fun testAppNetworkUsageEquality() {
        val attrs = Attributes.empty()
        val usage1 = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100L, 200L, attrs)
        val usage2 = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100L, 200L, attrs)
        assertEquals(usage1, usage2)
    }

    @Test
    fun testAppNetworkUsageCopy() {
        val usage1 =
            AppNetworkUsage(
                packageName = "com.test",
                uid = 1000,
                networkType = "OEM_PAID",
                bytesTransmitted = 100L,
                bytesReceived = 200L,
                attributes = Attributes.empty(),
            )
        val usage2 = usage1.copy(bytesTransmitted = 500L)

        assertEquals(500L, usage2.bytesTransmitted)
        assertEquals(usage1.bytesReceived, usage2.bytesReceived)
    }

    @Test
    fun testAppNetworkUsageToString() {
        val usage =
            AppNetworkUsage(
                packageName = "com.test.app",
                uid = 1000,
                networkType = "OEM_PAID",
                bytesTransmitted = 100L,
                bytesReceived = 200L,
                attributes = Attributes.empty(),
            )
        val str = usage.toString()

        assertTrue(str.contains("com.test.app"))
        assertTrue(str.contains("1000"))
        assertTrue(str.contains("OEM_PAID"))
    }

    // ==================== PreferenceChange Data Class ====================

    @Test
    fun testPreferenceChangeCreation() {
        val timestamp = System.currentTimeMillis()
        val change =
            PreferenceChange(
                packageName = "com.test.app",
                uid = 1000,
                oldPreference = "OEM_PAID",
                newPreference = "OEM_PRIVATE",
                timestamp = timestamp,
                attributes = Attributes.empty(),
            )

        assertEquals("com.test.app", change.packageName)
        assertEquals(1000, change.uid)
        assertEquals("OEM_PAID", change.oldPreference)
        assertEquals("OEM_PRIVATE", change.newPreference)
        assertEquals(timestamp, change.timestamp)
    }

    @Test
    fun testPreferenceChangeDefaultTimestamp() {
        val before = System.currentTimeMillis()
        val change =
            PreferenceChange(
                packageName = "com.test",
                uid = 1000,
                oldPreference = "A",
                newPreference = "B",
                attributes = Attributes.empty(),
            )
        val after = System.currentTimeMillis()

        assertTrue(change.timestamp >= before)
        assertTrue(change.timestamp <= after)
    }

    @Test
    fun testPreferenceChangeEquality() {
        val timestamp = System.currentTimeMillis()
        val attrs = Attributes.empty()
        val change1 = PreferenceChange("com.test", 1000, "A", "B", timestamp, attrs)
        val change2 = PreferenceChange("com.test", 1000, "A", "B", timestamp, attrs)
        assertEquals(change1, change2)
    }

    @Test
    fun testPreferenceChangeCopy() {
        val change1 =
            PreferenceChange(
                packageName = "com.test",
                uid = 1000,
                oldPreference = "A",
                newPreference = "B",
                timestamp = System.currentTimeMillis(),
                attributes = Attributes.empty(),
            )
        val change2 = change1.copy(newPreference = "C")

        assertEquals("C", change2.newPreference)
        assertEquals(change1.oldPreference, change2.oldPreference)
    }

    // ==================== NetworkAvailability Data Class ====================

    @Test
    fun testNetworkAvailabilityCreation() {
        val availability =
            NetworkAvailability(
                networkType = "OEM_PAID",
                isAvailable = true,
                signalStrength = 80,
                attributes = Attributes.empty(),
            )

        assertEquals("OEM_PAID", availability.networkType)
        assertTrue(availability.isAvailable)
        assertEquals(80, availability.signalStrength)
    }

    @Test
    fun testNetworkAvailabilityDefaultSignalStrength() {
        val availability =
            NetworkAvailability(
                networkType = "OEM_PAID",
                isAvailable = true,
                attributes = Attributes.empty(),
            )

        assertEquals(-1, availability.signalStrength)
    }

    @Test
    fun testNetworkAvailabilityNotAvailable() {
        val availability =
            NetworkAvailability(
                networkType = "OEM_PRIVATE",
                isAvailable = false,
                attributes = Attributes.empty(),
            )

        assertFalse(availability.isAvailable)
    }

    @Test
    fun testNetworkAvailabilityEquality() {
        val attrs = Attributes.empty()
        val avail1 = NetworkAvailability("OEM_PAID", true, -1, attrs)
        val avail2 = NetworkAvailability("OEM_PAID", true, -1, attrs)
        assertEquals(avail1, avail2)
    }

    @Test
    fun testNetworkAvailabilityCopy() {
        val avail1 =
            NetworkAvailability(
                networkType = "OEM_PAID",
                isAvailable = true,
                signalStrength = 80,
                attributes = Attributes.empty(),
            )
        val avail2 = avail1.copy(isAvailable = false)

        assertFalse(avail2.isAvailable)
        assertEquals(avail1.signalStrength, avail2.signalStrength)
    }

    // ==================== buildPansAttributes() ====================

    @Test
    fun testBuildPansAttributesBasic() {
        val attrs =
            buildPansAttributes(
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                uid = 1000,
            )

        assertNotNull(attrs)
        assertEquals("com.test.app", attrs.get(AttributeKey.stringKey("app_package_name")))
        assertEquals("OEM_PAID", attrs.get(AttributeKey.stringKey("network_type")))
        assertEquals(1000L, attrs.get(AttributeKey.longKey("uid")))
    }

    @Test
    fun testBuildPansAttributesWithAdditionalBuilder() {
        val attrs =
            buildPansAttributes(
                packageName = "com.test.app",
                networkType = "OEM_PAID",
                uid = 1000,
            ) { builder ->
                builder.put("custom_key", "custom_value")
                builder.put("timestamp_ms", 123456789L)
            }

        assertNotNull(attrs)
        assertEquals("custom_value", attrs.get(AttributeKey.stringKey("custom_key")))
        assertEquals(123456789L, attrs.get(AttributeKey.longKey("timestamp_ms")))
    }

    @Test
    fun testBuildPansAttributesWithEmptyPackageName() {
        val attrs =
            buildPansAttributes(
                packageName = "",
                networkType = "OEM_PAID",
                uid = 1000,
            )

        assertEquals("", attrs.get(AttributeKey.stringKey("app_package_name")))
    }

    @Test
    fun testBuildPansAttributesWithNegativeUid() {
        val attrs =
            buildPansAttributes(
                packageName = "com.test",
                networkType = "OEM_PAID",
                uid = -1,
            )

        assertEquals(-1L, attrs.get(AttributeKey.longKey("uid")))
    }

    // ==================== buildPreferenceChangeAttributes() ====================

    @Test
    fun testBuildPreferenceChangeAttributesBasic() {
        val attrs =
            buildPreferenceChangeAttributes(
                packageName = "com.test.app",
                oldPreference = "OEM_PAID",
                newPreference = "OEM_PRIVATE",
                uid = 1000,
            )

        assertNotNull(attrs)
        assertEquals("com.test.app", attrs.get(AttributeKey.stringKey("app_package_name")))
        assertEquals("OEM_PAID", attrs.get(AttributeKey.stringKey("old_preference")))
        assertEquals("OEM_PRIVATE", attrs.get(AttributeKey.stringKey("new_preference")))
        assertEquals(1000L, attrs.get(AttributeKey.longKey("uid")))
    }

    @Test
    fun testBuildPreferenceChangeAttributesWithEmptyStrings() {
        val attrs =
            buildPreferenceChangeAttributes(
                packageName = "",
                oldPreference = "",
                newPreference = "",
                uid = 0,
            )

        assertEquals("", attrs.get(AttributeKey.stringKey("app_package_name")))
        assertEquals("", attrs.get(AttributeKey.stringKey("old_preference")))
        assertEquals("", attrs.get(AttributeKey.stringKey("new_preference")))
    }

    // ==================== buildNetworkAvailabilityAttributes() ====================

    @Test
    fun testBuildNetworkAvailabilityAttributesBasic() {
        val attrs =
            buildNetworkAvailabilityAttributes(
                networkType = "OEM_PAID",
            )

        assertNotNull(attrs)
        assertEquals("OEM_PAID", attrs.get(AttributeKey.stringKey("network_type")))
        // Signal strength should not be present when -1
        assertNull(attrs.get(AttributeKey.longKey("signal_strength")))
    }

    @Test
    fun testBuildNetworkAvailabilityAttributesWithSignalStrength() {
        val attrs =
            buildNetworkAvailabilityAttributes(
                networkType = "OEM_PAID",
                signalStrength = 80,
            )

        assertEquals("OEM_PAID", attrs.get(AttributeKey.stringKey("network_type")))
        assertEquals(80L, attrs.get(AttributeKey.longKey("signal_strength")))
    }

    @Test
    fun testBuildNetworkAvailabilityAttributesWithZeroSignalStrength() {
        val attrs =
            buildNetworkAvailabilityAttributes(
                networkType = "OEM_PRIVATE",
                signalStrength = 0,
            )

        assertEquals(0L, attrs.get(AttributeKey.longKey("signal_strength")))
    }

    @Test
    fun testBuildNetworkAvailabilityAttributesWithNegativeSignalStrength() {
        val attrs =
            buildNetworkAvailabilityAttributes(
                networkType = "OEM_PAID",
                signalStrength = -1,
            )

        // Negative signal strength should not add the attribute
        assertNull(attrs.get(AttributeKey.longKey("signal_strength")))
    }

    @Test
    fun testBuildNetworkAvailabilityAttributesWithEmptyNetworkType() {
        val attrs =
            buildNetworkAvailabilityAttributes(
                networkType = "",
            )

        assertEquals("", attrs.get(AttributeKey.stringKey("network_type")))
    }
}
