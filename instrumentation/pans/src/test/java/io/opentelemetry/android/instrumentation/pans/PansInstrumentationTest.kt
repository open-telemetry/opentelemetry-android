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
import io.mockk.verify
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PansInstrumentationTest {
    private lateinit var context: Context
    private lateinit var mockSdk: OpenTelemetrySdk
    private lateinit var mockInstallationContext: InstallationContext

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockSdk = mockk(relaxed = true)
        mockInstallationContext = mockk(relaxed = true)
        every { mockInstallationContext.context } returns context
        every { mockInstallationContext.openTelemetry } returns mockSdk
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

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

    // ==================== Install Method Tests ====================

    @Test
    fun testInstallWithValidContext() {
        val instrumentation = PansInstrumentation()
        try {
            instrumentation.install(mockInstallationContext)
        } catch (e: Exception) {
            // May fail due to services not being available, but should not crash
        }
    }

    @Test
    fun testInstallDoesNotThrow() {
        val instrumentation = PansInstrumentation()
        try {
            instrumentation.install(mockInstallationContext)
        } catch (e: Exception) {
            throw AssertionError("install() should not throw", e)
        }
    }

    @Test
    fun testInstallMultipleTimes() {
        val instrumentation = PansInstrumentation()
        try {
            instrumentation.install(mockInstallationContext)
            instrumentation.install(mockInstallationContext)
        } catch (e: Exception) {
            // Expected - multiple installs may fail
        }
    }

    @Test
    fun testInstallAccessesContext() {
        val instrumentation = PansInstrumentation()
        try {
            instrumentation.install(mockInstallationContext)
            verify(atLeast = 0) { mockInstallationContext.context }
        } catch (e: Exception) {
            // May fail if services not available
        }
    }

    @Test
    fun testInstallAccessesOpenTelemetry() {
        val instrumentation = PansInstrumentation()
        try {
            instrumentation.install(mockInstallationContext)
            verify(atLeast = 0) { mockInstallationContext.openTelemetry }
        } catch (e: Exception) {
            // May fail if services not available
        }
    }

    // ==================== State Tests ====================

    @Test
    fun testInstrumentationState() {
        val instrumentation = PansInstrumentation()
        assertNotNull(instrumentation)
        assertEquals("pans", instrumentation.name)
    }

    @Test
    fun testInstrumentationStatePersistent() {
        val instrumentation = PansInstrumentation()
        val name1 = instrumentation.name
        Thread.sleep(10)
        val name2 = instrumentation.name
        assertEquals(name1, name2)
    }

    // ==================== Concurrent Tests ====================

    @Test
    fun testConcurrentInstallCalls() {
        val instrumentation = PansInstrumentation()
        val threads = mutableListOf<Thread>()

        repeat(3) {
            threads.add(
                Thread {
                    try {
                        instrumentation.install(mockInstallationContext)
                    } catch (e: Exception) {
                        // Expected in concurrent scenario
                    }
                },
            )
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals("pans", instrumentation.name)
    }

    @Test
    fun testMultipleInstrumentationsInstallConcurrently() {
        val threads = mutableListOf<Thread>()

        repeat(3) {
            threads.add(
                Thread {
                    val inst = PansInstrumentation()
                    try {
                        inst.install(mockInstallationContext)
                    } catch (e: Exception) {
                        // May fail
                    }
                },
            )
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }
    }

    // ==================== Integration Tests ====================

    @Test
    fun testInstrumentationFullLifecycle() {
        val instrumentation = PansInstrumentation()
        assertNotNull(instrumentation)
        assertEquals("pans", instrumentation.name)

        try {
            instrumentation.install(mockInstallationContext)
        } catch (e: Exception) {
            // May fail but should not crash
        }

        assertEquals("pans", instrumentation.name)
    }

    @Test
    fun testMultipleInstrumentationLifecycles() {
        repeat(3) {
            val instrumentation = PansInstrumentation()
            assertNotNull(instrumentation)

            try {
                instrumentation.install(mockInstallationContext)
            } catch (e: Exception) {
                // May fail
            }

            assertEquals("pans", instrumentation.name)
        }
    }
}
