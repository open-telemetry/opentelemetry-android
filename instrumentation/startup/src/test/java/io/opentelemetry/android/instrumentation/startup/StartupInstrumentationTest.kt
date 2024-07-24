/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class StartupInstrumentationTest {
    @JvmField
    @RegisterExtension
    val otelTesting = OpenTelemetryExtension.create()
    private lateinit var instrumentation: StartupInstrumentation

    @BeforeEach
    fun setUp() {
        instrumentation = StartupInstrumentation()
    }

    @AfterEach
    fun tearDown() {
        InitializationEvents.resetForTest()
    }

    @Test
    fun `Call finish on SdkInitializationEvents`() {
        val sdkInitializationEvents = mockk<SdkInitializationEvents>()
        val openTelemetryRum = mockk<OpenTelemetryRum>()
        every { openTelemetryRum.openTelemetry }.returns(otelTesting.openTelemetry)
        every { sdkInitializationEvents.finish(any()) } just Runs
        InitializationEvents.set(sdkInitializationEvents)

        instrumentation.install(mockk(), openTelemetryRum)

        verify {
            sdkInitializationEvents.finish(otelTesting.openTelemetry)
        }
    }

    @Test
    fun `No action when the InitializationEvents instance is not SdkInitializationEvents`() {
        val initializationEvents = mockk<InitializationEvents>()
        val openTelemetryRum = mockk<OpenTelemetryRum>()
        InitializationEvents.set(initializationEvents)

        instrumentation.install(mockk(), openTelemetryRum)

        verify { initializationEvents wasNot Called }
    }
}
