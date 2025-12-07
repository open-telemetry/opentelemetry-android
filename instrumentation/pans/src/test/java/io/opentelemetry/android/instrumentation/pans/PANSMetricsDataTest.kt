/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for PANSMetrics data class and attribute builder functions.
 * Note: Tests for AppNetworkUsage, PreferenceChange, and NetworkAvailability
 * are in separate test files to avoid exceeding class size limits.
 */
class PANSMetricsDataTest {
    // ==================== PANSMetrics Data Class Tests ====================

    @Test
    fun testPANSMetricsDefaultConstructor() {
        val metrics = PANSMetrics()
        assertTrue(metrics.appNetworkUsage.isEmpty())
        assertTrue(metrics.preferenceChanges.isEmpty())
        assertTrue(metrics.networkAvailability.isEmpty())
    }

    @Test
    fun testPANSMetricsWithEmptyLists() {
        val metrics =
            PANSMetrics(
                appNetworkUsage = emptyList(),
                preferenceChanges = emptyList(),
                networkAvailability = emptyList(),
            )
        assertEquals(0, metrics.appNetworkUsage.size)
        assertEquals(0, metrics.preferenceChanges.size)
        assertEquals(0, metrics.networkAvailability.size)
    }

    @Test
    fun testPANSMetricsWithSingleAppUsage() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100, 200, attrs)
        val metrics = PANSMetrics(appNetworkUsage = listOf(usage))
        assertEquals(1, metrics.appNetworkUsage.size)
        assertEquals("com.test", metrics.appNetworkUsage[0].packageName)
    }

    @Test
    fun testPANSMetricsWithMultipleAppUsages() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usages =
            listOf(
                AppNetworkUsage("com.app1", 1000, "OEM_PAID", 100, 200, attrs),
                AppNetworkUsage("com.app2", 1001, "OEM_PRIVATE", 300, 400, attrs),
                AppNetworkUsage("com.app3", 1002, "OEM_PAID", 500, 600, attrs),
            )
        val metrics = PANSMetrics(appNetworkUsage = usages)
        assertEquals(3, metrics.appNetworkUsage.size)
    }

    @Test
    fun testPANSMetricsWithPreferenceChanges() {
        val attrs = Attributes.builder().put("test", "value").build()
        val changes =
            listOf(
                PreferenceChange("com.app1", 1000, "OLD", "NEW", 123L, attrs),
            )
        val metrics = PANSMetrics(preferenceChanges = changes)
        assertEquals(1, metrics.preferenceChanges.size)
    }

    @Test
    fun testPANSMetricsWithNetworkAvailability() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val availability =
            listOf(
                NetworkAvailability("OEM_PAID", true, 90, attrs),
                NetworkAvailability("OEM_PRIVATE", false, -1, attrs),
            )
        val metrics = PANSMetrics(networkAvailability = availability)
        assertEquals(2, metrics.networkAvailability.size)
    }

    @Test
    fun testPANSMetricsEquality() {
        val metrics1 = PANSMetrics()
        val metrics2 = PANSMetrics()
        assertEquals(metrics1, metrics2)
    }

    @Test
    fun testPANSMetricsCopy() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100, 200, attrs)
        val metrics1 = PANSMetrics(appNetworkUsage = listOf(usage))
        val metrics2 = metrics1.copy()
        assertEquals(metrics1, metrics2)
    }

    // ==================== Attribute Builder Function Tests ====================

    @Test
    fun testBuildPansAttributesBasic() {
        val attrs = buildPansAttributes("com.test", "OEM_PAID", 1000)
        assertNotNull(attrs)
        assertEquals(
            "com.test",
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .stringKey("app_package_name"),
            ),
        )
        assertEquals(
            "OEM_PAID",
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .stringKey("network_type"),
            ),
        )
        assertEquals(
            1000L,
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .longKey("uid"),
            ),
        )
    }

    @Test
    fun testBuildPansAttributesWithAdditionalBuilder() {
        val attrs =
            buildPansAttributes("com.test", "OEM_PAID", 1000) { builder ->
                builder.put("custom_key", "custom_value")
            }
        assertNotNull(attrs)
        assertEquals(
            "custom_value",
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .stringKey("custom_key"),
            ),
        )
    }

    @Test
    fun testBuildPansAttributesEmptyPackage() {
        val attrs = buildPansAttributes("", "OEM_PAID", 1000)
        assertNotNull(attrs)
        assertEquals(
            "",
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .stringKey("app_package_name"),
            ),
        )
    }

    @Test
    fun testBuildPreferenceChangeAttributesBasic() {
        val attrs = buildPreferenceChangeAttributes("com.test", "OLD", "NEW", 1000)
        assertNotNull(attrs)
        assertEquals(
            "com.test",
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .stringKey("app_package_name"),
            ),
        )
        assertEquals(
            "OLD",
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .stringKey("old_preference"),
            ),
        )
        assertEquals(
            "NEW",
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .stringKey("new_preference"),
            ),
        )
    }

    @Test
    fun testBuildNetworkAvailabilityAttributesWithSignal() {
        val attrs = buildNetworkAvailabilityAttributes("OEM_PAID", 95)
        assertNotNull(attrs)
        assertEquals(
            "OEM_PAID",
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .stringKey("network_type"),
            ),
        )
        assertEquals(
            95L,
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .longKey("signal_strength"),
            ),
        )
    }

    @Test
    fun testBuildNetworkAvailabilityAttributesNoSignal() {
        val attrs = buildNetworkAvailabilityAttributes("OEM_PAID", -1)
        assertNotNull(attrs)
        assertEquals(
            "OEM_PAID",
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .stringKey("network_type"),
            ),
        )
        // signal_strength should NOT be present when -1
        assertEquals(
            null,
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .longKey("signal_strength"),
            ),
        )
    }

    @Test
    fun testBuildNetworkAvailabilityAttributesDefaultSignal() {
        val attrs = buildNetworkAvailabilityAttributes("OEM_PAID")
        assertNotNull(attrs)
        assertEquals(
            "OEM_PAID",
            attrs.get(
                io.opentelemetry.api.common.AttributeKey
                    .stringKey("network_type"),
            ),
        )
    }
}
