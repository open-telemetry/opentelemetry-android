/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkAvailabilityTest {
    @Test
    fun testNetworkAvailabilityCreation() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val availability =
            NetworkAvailability(
                networkType = "OEM_PAID",
                isAvailable = true,
                signalStrength = 95,
                attributes = attrs,
            )
        assertEquals("OEM_PAID", availability.networkType)
        assertTrue(availability.isAvailable)
        assertEquals(95, availability.signalStrength)
        assertNotNull(availability.attributes)
    }

    @Test
    fun testNetworkAvailabilityNotAvailable() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val availability = NetworkAvailability("OEM_PAID", false, attributes = attrs)
        assertFalse(availability.isAvailable)
    }

    @Test
    fun testNetworkAvailabilityDefaultSignalStrength() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val availability = NetworkAvailability("OEM_PAID", true, attributes = attrs)
        assertEquals(-1, availability.signalStrength)
    }

    @Test
    fun testNetworkAvailabilityZeroSignalStrength() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val availability = NetworkAvailability("OEM_PAID", true, 0, attrs)
        assertEquals(0, availability.signalStrength)
    }

    @Test
    fun testNetworkAvailabilityMaxSignalStrength() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val availability = NetworkAvailability("OEM_PAID", true, 100, attrs)
        assertEquals(100, availability.signalStrength)
    }

    @Test
    fun testNetworkAvailabilityOEMPrivateType() {
        val attrs = Attributes.builder().put("network_type", "OEM_PRIVATE").build()
        val availability = NetworkAvailability("OEM_PRIVATE", true, attributes = attrs)
        assertEquals("OEM_PRIVATE", availability.networkType)
    }

    @Test
    fun testNetworkAvailabilityEquality() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val avail1 = NetworkAvailability("OEM_PAID", true, 90, attrs)
        val avail2 = NetworkAvailability("OEM_PAID", true, 90, attrs)
        assertEquals(avail1, avail2)
    }

    @Test
    fun testNetworkAvailabilityInequalityByType() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val avail1 = NetworkAvailability("OEM_PAID", true, 90, attrs)
        val avail2 = NetworkAvailability("OEM_PRIVATE", true, 90, attrs)
        assertNotEquals(avail1, avail2)
    }

    @Test
    fun testNetworkAvailabilityInequalityByAvailability() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val avail1 = NetworkAvailability("OEM_PAID", true, 90, attrs)
        val avail2 = NetworkAvailability("OEM_PAID", false, 90, attrs)
        assertNotEquals(avail1, avail2)
    }

    @Test
    fun testNetworkAvailabilityCopy() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val avail1 = NetworkAvailability("OEM_PAID", true, 90, attrs)
        val avail2 = avail1.copy(isAvailable = false)
        assertFalse(avail2.isAvailable)
        assertTrue(avail1.isAvailable)
    }

    @Test
    fun testNetworkAvailabilityHashCode() {
        val attrs = Attributes.builder().put("network_type", "OEM_PAID").build()
        val avail1 = NetworkAvailability("OEM_PAID", true, 90, attrs)
        val avail2 = NetworkAvailability("OEM_PAID", true, 90, attrs)
        assertEquals(avail1.hashCode(), avail2.hashCode())
    }
}
