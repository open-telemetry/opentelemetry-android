/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import android.content.Context
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.android.session.SessionPublisher
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SessionInstrumentationTest {
    private interface PublishingSessionProvider :
        SessionProvider,
        SessionPublisher

    private val context = mockk<Context>()
    private val rum = mockk<OpenTelemetryRum>()
    private val publisher = mockk<PublishingSessionProvider>()
    private val observers = mutableListOf<SessionObserver>()
    private val instrumentation = SessionInstrumentation()

    private companion object {
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    @BeforeEach
    fun setup() {
        every { rum.openTelemetry } returns otelTesting.openTelemetry
        every { rum.sessionProvider } returns publisher
        every { publisher.addObserver(capture(observers)) } just Runs
        every { publisher.removeObserver(any()) } just Runs
    }

    @Test
    fun `uninstall removes the registered observer only once`() {
        instrumentation.install(context, rum)
        instrumentation.uninstall(context, rum)
        instrumentation.uninstall(context, rum)

        verify(exactly = 1) { publisher.addObserver(observers.single()) }
        verify(exactly = 1) { publisher.removeObserver(observers.single()) }
    }

    @Test
    fun `uninstall is safe before install`() {
        instrumentation.uninstall(context, rum)

        verify(exactly = 0) { publisher.addObserver(any()) }
        verify(exactly = 0) { publisher.removeObserver(any()) }
    }

    @Test
    fun `uninstall is safe when session provider does not publish`() {
        every { rum.sessionProvider } returns SessionProvider.getNoop()

        instrumentation.install(context, rum)
        instrumentation.uninstall(context, rum)

        verify(exactly = 0) { publisher.addObserver(any()) }
        verify(exactly = 0) { publisher.removeObserver(any()) }
    }

    @Test
    fun `install replaces previous observer`() {
        instrumentation.install(context, rum)
        instrumentation.install(context, rum)
        instrumentation.uninstall(context, rum)

        verifyOrder {
            publisher.addObserver(observers[0])
            publisher.removeObserver(observers[0])
            publisher.addObserver(observers[1])
            publisher.removeObserver(observers[1])
        }
        verify(exactly = 1) { publisher.removeObserver(observers[0]) }
        verify(exactly = 1) { publisher.removeObserver(observers[1]) }
    }
}
