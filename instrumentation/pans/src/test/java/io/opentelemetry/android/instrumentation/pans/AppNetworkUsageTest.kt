/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNetworkUsageTest {
    @Test
    fun testAppNetworkUsageCreation() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage =
            AppNetworkUsage(
                packageName = "com.example.app",
                uid = 1000,
                networkType = "OEM_PAID",
                bytesTransmitted = 1024,
                bytesReceived = 2048,
                attributes = attrs,
            )
        assertEquals("com.example.app", usage.packageName)
        assertEquals(1000, usage.uid)
        assertEquals("OEM_PAID", usage.networkType)
        assertEquals(1024L, usage.bytesTransmitted)
        assertEquals(2048L, usage.bytesReceived)
        assertNotNull(usage.attributes)
    }

    @Test
    fun testAppNetworkUsageWithZeroBytes() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage = AppNetworkUsage("com.test", 1000, "OEM_PAID", 0, 0, attrs)
        assertEquals(0L, usage.bytesTransmitted)
        assertEquals(0L, usage.bytesReceived)
    }

    @Test
    fun testAppNetworkUsageWithMaxBytes() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage = AppNetworkUsage("com.test", 1000, "OEM_PAID", Long.MAX_VALUE, Long.MAX_VALUE, attrs)
        assertEquals(Long.MAX_VALUE, usage.bytesTransmitted)
        assertEquals(Long.MAX_VALUE, usage.bytesReceived)
    }

    @Test
    fun testAppNetworkUsageWithMinUID() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage = AppNetworkUsage("com.test", 0, "OEM_PAID", 100, 200, attrs)
        assertEquals(0, usage.uid)
    }

    @Test
    fun testAppNetworkUsageWithMaxUID() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage = AppNetworkUsage("com.test", Int.MAX_VALUE, "OEM_PAID", 100, 200, attrs)
        assertEquals(Int.MAX_VALUE, usage.uid)
    }

    @Test
    fun testAppNetworkUsageOEMPaidType() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100, 200, attrs)
        assertEquals("OEM_PAID", usage.networkType)
    }

    @Test
    fun testAppNetworkUsageOEMPrivateType() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage = AppNetworkUsage("com.test", 1000, "OEM_PRIVATE", 100, 200, attrs)
        assertEquals("OEM_PRIVATE", usage.networkType)
    }

    @Test
    fun testAppNetworkUsageEquality() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage1 = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100, 200, attrs)
        val usage2 = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100, 200, attrs)
        assertEquals(usage1, usage2)
    }

    @Test
    fun testAppNetworkUsageInequalityByPackage() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage1 = AppNetworkUsage("com.test1", 1000, "OEM_PAID", 100, 200, attrs)
        val usage2 = AppNetworkUsage("com.test2", 1000, "OEM_PAID", 100, 200, attrs)
        assertNotEquals(usage1, usage2)
    }

    @Test
    fun testAppNetworkUsageCopy() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage1 = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100, 200, attrs)
        val usage2 = usage1.copy(bytesTransmitted = 500)
        assertEquals(500L, usage2.bytesTransmitted)
        assertEquals(100L, usage1.bytesTransmitted)
    }

    @Test
    fun testAppNetworkUsageHashCode() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage1 = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100, 200, attrs)
        val usage2 = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100, 200, attrs)
        assertEquals(usage1.hashCode(), usage2.hashCode())
    }

    @Test
    fun testAppNetworkUsageToString() {
        val attrs = Attributes.builder().put("test", "value").build()
        val usage = AppNetworkUsage("com.test", 1000, "OEM_PAID", 100, 200, attrs)
        val str = usage.toString()
        assertTrue(str.contains("com.test"))
        assertTrue(str.contains("OEM_PAID"))
    }
}
