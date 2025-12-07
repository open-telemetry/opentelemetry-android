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

class PreferenceChangeTest {
    @Test
    fun testPreferenceChangeCreation() {
        val attrs = Attributes.builder().put("test", "value").build()
        val change =
            PreferenceChange(
                packageName = "com.example.app",
                uid = 1000,
                oldPreference = "OEM_PAID",
                newPreference = "OEM_PRIVATE",
                attributes = attrs,
            )
        assertEquals("com.example.app", change.packageName)
        assertEquals(1000, change.uid)
        assertEquals("OEM_PAID", change.oldPreference)
        assertEquals("OEM_PRIVATE", change.newPreference)
        assertNotNull(change.attributes)
    }

    @Test
    fun testPreferenceChangeWithExplicitTimestamp() {
        val attrs = Attributes.builder().put("test", "value").build()
        val timestamp = 1234567890L
        val change = PreferenceChange("com.test", 1000, "OLD", "NEW", timestamp, attrs)
        assertEquals(timestamp, change.timestamp)
    }

    @Test
    fun testPreferenceChangeDefaultTimestamp() {
        val attrs = Attributes.builder().put("test", "value").build()
        val before = System.currentTimeMillis()
        val change = PreferenceChange("com.test", 1000, "OLD", "NEW", attributes = attrs)
        val after = System.currentTimeMillis()
        assertTrue(change.timestamp >= before)
        assertTrue(change.timestamp <= after)
    }

    @Test
    fun testPreferenceChangeSamePreference() {
        val attrs = Attributes.builder().put("test", "value").build()
        val change = PreferenceChange("com.test", 1000, "OEM_PAID", "OEM_PAID", attributes = attrs)
        assertEquals(change.oldPreference, change.newPreference)
    }

    @Test
    fun testPreferenceChangeEquality() {
        val attrs = Attributes.builder().put("test", "value").build()
        val change1 = PreferenceChange("com.test", 1000, "OLD", "NEW", 100L, attrs)
        val change2 = PreferenceChange("com.test", 1000, "OLD", "NEW", 100L, attrs)
        assertEquals(change1, change2)
    }

    @Test
    fun testPreferenceChangeInequalityByOldPref() {
        val attrs = Attributes.builder().put("test", "value").build()
        val change1 = PreferenceChange("com.test", 1000, "OLD1", "NEW", 100L, attrs)
        val change2 = PreferenceChange("com.test", 1000, "OLD2", "NEW", 100L, attrs)
        assertNotEquals(change1, change2)
    }

    @Test
    fun testPreferenceChangeCopy() {
        val attrs = Attributes.builder().put("test", "value").build()
        val change1 = PreferenceChange("com.test", 1000, "OLD", "NEW", 100L, attrs)
        val change2 = change1.copy(newPreference = "UPDATED")
        assertEquals("UPDATED", change2.newPreference)
        assertEquals("NEW", change1.newPreference)
    }

    @Test
    fun testPreferenceChangeHashCode() {
        val attrs = Attributes.builder().put("test", "value").build()
        val change1 = PreferenceChange("com.test", 1000, "OLD", "NEW", 100L, attrs)
        val change2 = PreferenceChange("com.test", 1000, "OLD", "NEW", 100L, attrs)
        assertEquals(change1.hashCode(), change2.hashCode())
    }
}
