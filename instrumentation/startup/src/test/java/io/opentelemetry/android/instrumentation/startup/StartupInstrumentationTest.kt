/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import android.app.Application
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.android.session.SessionProvider
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
        every { sdkInitializationEvents.finish(any(), any()) } just Runs
        InitializationEvents.set(sdkInitializationEvents)

        instrumentation.install(makeContext())

        verify {
            sdkInitializationEvents.finish(otelTesting.openTelemetry, any())
        }
    }

    @Test
    fun `No action when the InitializationEvents instance is not SdkInitializationEvents`() {
        val initializationEvents = mockk<InitializationEvents>()
        InitializationEvents.set(initializationEvents)

        instrumentation.install(makeContext())

        verify { initializationEvents wasNot Called }
    }

    private fun makeContext(): InstallationContext =
        InstallationContext(
            mockk<Application>(),
            otelTesting.openTelemetry,
            SessionProvider.getNoop(),
        )
}
