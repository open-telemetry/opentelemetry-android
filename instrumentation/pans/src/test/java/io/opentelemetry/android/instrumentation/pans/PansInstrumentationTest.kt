/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PansInstrumentationTest {
    // ==================== Name Property Tests ====================

    @Test
    fun testInstrumentationName() {
        val instrumentation = PansInstrumentation()
        assertEquals("pans", instrumentation.name)
    }

    @Test
    fun testInstrumentationNameNotNull() {
        val instrumentation = PansInstrumentation()
        assertNotNull(instrumentation.name)
    }

    @Test
    fun testInstrumentationNameNotEmpty() {
        val instrumentation = PansInstrumentation()
        assert(instrumentation.name.isNotEmpty())
    }

    @Test
    fun testInstrumentationNameIsLowercase() {
        val instrumentation = PansInstrumentation()
        assertEquals(instrumentation.name, instrumentation.name.lowercase())
    }

    // ==================== Instance Creation Tests ====================

    @Test
    fun testInstrumentationCreation() {
        val instrumentation = PansInstrumentation()
        assertNotNull(instrumentation)
    }

    @Test
    fun testMultipleInstrumentationInstances() {
        val inst1 = PansInstrumentation()
        val inst2 = PansInstrumentation()
        assertNotNull(inst1)
        assertNotNull(inst2)
    }

    @Test
    fun testInstrumentationInstancesHaveSameName() {
        val inst1 = PansInstrumentation()
        val inst2 = PansInstrumentation()
        assertEquals(inst1.name, inst2.name)
    }

    // ==================== Instrumentation Interface Tests ====================

    @Test
    fun testInstrumentationImplementsInterface() {
        val instrumentation = PansInstrumentation()
        assert(instrumentation is io.opentelemetry.android.instrumentation.AndroidInstrumentation)
    }

    @Test
    fun testInstrumentationNameProperty() {
        val instrumentation: io.opentelemetry.android.instrumentation.AndroidInstrumentation = PansInstrumentation()
        assertEquals("pans", instrumentation.name)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testInstrumentationNameLength() {
        val instrumentation = PansInstrumentation()
        assertEquals(4, instrumentation.name.length)
    }

    @Test
    fun testInstrumentationNameContainsPans() {
        val instrumentation = PansInstrumentation()
        assert(instrumentation.name.contains("pans"))
    }

    @Test
    fun testInstrumentationNameDoesNotContainSpaces() {
        val instrumentation = PansInstrumentation()
        assert(!instrumentation.name.contains(" "))
    }

    @Test
    fun testInstrumentationNameDoesNotContainSpecialChars() {
        val instrumentation = PansInstrumentation()
        assert(instrumentation.name.all { it.isLetterOrDigit() || it == '_' || it == '-' })
    }

    // ==================== Rapid Creation Tests ====================

    @Test
    fun testRapidInstrumentationCreation() {
        val instrumentations = mutableListOf<PansInstrumentation>()
        repeat(10) {
            instrumentations.add(PansInstrumentation())
        }
        assertEquals(10, instrumentations.size)
        instrumentations.forEach { assertEquals("pans", it.name) }
    }

    @Test
    fun testInstrumentationCreationDoesNotThrow() {
        try {
            PansInstrumentation()
        } catch (e: Exception) {
            throw AssertionError("Creating PansInstrumentation should not throw", e)
        }
    }
}
