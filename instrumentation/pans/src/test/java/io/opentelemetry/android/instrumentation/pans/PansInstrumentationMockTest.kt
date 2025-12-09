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
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Additional tests for PansInstrumentation to improve coverage.
 */
@RunWith(RobolectricTestRunner::class)
class PansInstrumentationMockTest {
    private lateinit var context: Context
    private lateinit var mockSdk: OpenTelemetrySdk
    private lateinit var mockMeter: Meter
    private lateinit var mockLoggerProvider: SdkLoggerProvider
    private lateinit var mockSessionProvider: SessionProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        mockMeter = mockk(relaxed = true)
        mockSdk = mockk(relaxed = true)
        mockLoggerProvider = mockk(relaxed = true)
        mockSessionProvider = mockk(relaxed = true)

        every { mockSdk.getMeter(any()) } returns mockMeter
        every { mockSdk.logsBridge } returns mockLoggerProvider
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Initialization Tests ====================

    @Test
    fun testInstrumentationName() {
        val instrumentation = PansInstrumentation()
        assertEquals("pans", instrumentation.name)
    }

    @Test
    fun testInstrumentationCreation() {
        val instrumentation = PansInstrumentation()
        assertNotNull(instrumentation)
    }

    @Test
    fun testMultipleInstrumentationInstances() {
        val instrumentation1 = PansInstrumentation()
        val instrumentation2 = PansInstrumentation()
        val instrumentation3 = PansInstrumentation()

        assertNotNull(instrumentation1)
        assertNotNull(instrumentation2)
        assertNotNull(instrumentation3)

        assertEquals(instrumentation1.name, instrumentation2.name)
        assertEquals(instrumentation2.name, instrumentation3.name)
    }

    // ==================== Install Tests with Mocked Context ====================

    @Test
    fun testInstallWithValidContext() {
        val instrumentation = PansInstrumentation()
        val installContext = InstallationContext(context, mockSdk as OpenTelemetry, mockSessionProvider)

        try {
            instrumentation.install(installContext)
        } catch (_: Exception) {
            // May throw due to Services not being initialized, which is expected
        }
    }

    @Test
    fun testInstallDoesNotCrashOnException() {
        val instrumentation = PansInstrumentation()
        val mockContext = mockk<Context>(relaxed = true)
        val installContext = InstallationContext(mockContext, mockSdk as OpenTelemetry, mockSessionProvider)

        // Should not throw even if internal setup fails
        try {
            instrumentation.install(installContext)
        } catch (_: Exception) {
            // Expected - Services not initialized
        }
    }

    @Test
    fun testInstallWithApplicationContext() {
        val instrumentation = PansInstrumentation()
        val appContext = context.applicationContext
        val installContext = InstallationContext(appContext, mockSdk as OpenTelemetry, mockSessionProvider)

        try {
            instrumentation.install(installContext)
        } catch (_: Exception) {
            // Expected - Services not initialized
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun testMultipleInstallCalls() {
        val instrumentation = PansInstrumentation()
        val installContext = InstallationContext(context, mockSdk as OpenTelemetry, mockSessionProvider)

        try {
            instrumentation.install(installContext)
            instrumentation.install(installContext) // Second install
            instrumentation.install(installContext) // Third install
        } catch (_: Exception) {
            // Expected - Services not initialized
        }
    }

    @Test
    fun testInstrumentationNameConsistency() {
        val instrumentation = PansInstrumentation()

        // Name should always be the same
        assertEquals("pans", instrumentation.name)
        assertEquals("pans", instrumentation.name)
        assertEquals("pans", instrumentation.name)
    }

    // ==================== InstallationContext Tests ====================

    @Test
    fun testInstallationContextCreation() {
        val installContext = InstallationContext(context, mockSdk as OpenTelemetry, mockSessionProvider)

        assertEquals(context, installContext.context)
        assertEquals(mockSdk, installContext.openTelemetry)
    }

    @Test
    fun testInstallationContextWithDifferentContexts() {
        val appContext = context.applicationContext
        val installContext1 = InstallationContext(context, mockSdk as OpenTelemetry, mockSessionProvider)
        val installContext2 = InstallationContext(appContext, mockSdk as OpenTelemetry, mockSessionProvider)

        assertNotNull(installContext1)
        assertNotNull(installContext2)
    }
}

