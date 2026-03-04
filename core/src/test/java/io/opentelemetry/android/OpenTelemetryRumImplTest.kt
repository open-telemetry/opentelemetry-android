/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.Clock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpenTelemetryRumImplTest {
    private val sdk: OpenTelemetrySdk = OpenTelemetrySdk.builder().build()
    private val sessionProvider: SessionProvider = mockk()
    private val context: Context = mockk()
    private val clock: Clock = mockk()
    private val onShutdown: Runnable = mockk(relaxed = true)

    private lateinit var rum: OpenTelemetryRumImpl

    @BeforeEach
    fun setUp() {
        every { sessionProvider.getSessionId() } returns "test-session-id"
        rum = OpenTelemetryRumImpl(sdk, sessionProvider, context, clock, onShutdown)
    }

    @Test
    fun `install delegates to instrumentation with correct context`() {
        val instrumentation = mockk<AndroidInstrumentation>(relaxed = true)

        rum.install(instrumentation)

        verify {
            instrumentation.install(
                match<InstallationContext> {
                    it.openTelemetry === sdk && it.sessionProvider === sessionProvider
                },
            )
        }
    }

    @Test
    fun `shutdown uninstalls manually installed instrumentations`() {
        val instrumentation1 = mockk<AndroidInstrumentation>(relaxed = true)
        val instrumentation2 = mockk<AndroidInstrumentation>(relaxed = true)

        rum.install(instrumentation1)
        rum.install(instrumentation2)
        rum.shutdown()

        verifyOrder {
            instrumentation1.uninstall(any())
            instrumentation2.uninstall(any())
            onShutdown.run()
        }
    }

    @Test
    fun `shutdown without manually installed instrumentations only runs onShutdown`() {
        val instrumentation = mockk<AndroidInstrumentation>(relaxed = true)

        rum.shutdown()

        verify { instrumentation wasNot Called }
        verify { onShutdown.run() }
    }

    @Test
    fun `getRumSessionId returns session id from provider`() {
        assertThat(rum.getRumSessionId()).isEqualTo("test-session-id")
    }

    @Test
    fun `openTelemetry returns the sdk`() {
        assertThat(rum.openTelemetry).isSameAs(sdk)
    }
}
