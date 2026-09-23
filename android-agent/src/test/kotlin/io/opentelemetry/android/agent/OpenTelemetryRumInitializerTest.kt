/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.android.session.SessionPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

@OptIn(Incubating::class)
@RunWith(AndroidJUnit4::class)
class OpenTelemetryRumInitializerTest {
    private lateinit var appLifecycle: AppLifecycle

    @Before
    fun setUp() {
        appLifecycle = mockk(relaxed = true)
        createAndSetServiceManager()
    }

    @After
    fun tearDown() {
        Services.set(null)
    }

    @Test
    fun `Verify timeoutHandler initialization 2`() {
        val rum =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    httpExport {
                        baseUrl = "http://127.0.0.1:4318"
                    }
                },
            )
        rum.shutdown()

        verify {
            appLifecycle.registerListener(any<SessionIdTimeoutHandler>())
        }
    }

    @Test
    fun `Verify session observers are applied`() {
        val o1: SessionObserver = mockk()
        val o2: SessionObserver = mockk()
        every { o1.onSessionStarted(any(), any()) } just Runs
        every { o1.onSessionEnded(any()) } just Runs
        every { o2.onSessionStarted(any(), any()) } just Runs
        every { o2.onSessionEnded(any()) } just Runs

        val rum =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    httpExport {
                        baseUrl = "http://127.0.0.1:4318"
                    }
                    session {
                        observers(o1, o2)
                    }
                },
            )
        rum.shutdown()

        verify {
            o1.onSessionStarted(any(), any())
            o2.onSessionStarted(any(), any())
        }
    }

    @Test
    fun `Verify a custom session provider is used`() {
        val rum =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    httpExport {
                        baseUrl = "http://127.0.0.1:4318"
                    }
                    session {
                        provider = SessionProvider { "custom-session-id" }
                    }
                },
            )
        rum.shutdown()

        assertThat(rum.sessionProvider.getSessionId()).isEqualTo("custom-session-id")
        verify(exactly = 0) {
            appLifecycle.registerListener(any<SessionIdTimeoutHandler>())
        }
    }

    @Test
    fun `Verify session observers are applied to a custom session publisher`() {
        val observer: SessionObserver = mockk()
        val provider = FakeSessionPublisher()

        OpenTelemetryRumInitializer
            .initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    httpExport {
                        baseUrl = "http://127.0.0.1:4318"
                    }
                    session {
                        this.provider = provider
                        observers(observer)
                    }
                },
            ).shutdown()
        assertThat(provider.observers).contains(observer)
    }

    @Test
    fun `Verify session observers are ignored by a custom session provider that cannot publish`() {
        val observer: SessionObserver = mockk()

        val rum =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    httpExport {
                        baseUrl = "http://127.0.0.1:4318"
                    }
                    session {
                        provider = SessionProvider { "custom-session-id" }
                        observers(observer)
                    }
                },
            )
        rum.shutdown()

        assertThat(rum.sessionProvider.getSessionId()).isEqualTo("custom-session-id")
        verify(exactly = 0) {
            observer.onSessionStarted(any(), any())
        }
    }

    private class FakeSessionPublisher :
        SessionProvider,
        SessionPublisher {
        val observers = mutableListOf<SessionObserver>()

        override fun getSessionId(): String = "custom-session-id"

        override fun addObserver(observer: SessionObserver) {
            observers.add(observer)
        }
    }

    private fun createAndSetServiceManager(): Services {
        val services = mockk<Services>()
        every { services.appLifecycle }.returns(appLifecycle)
        every { services.visibleScreenTracker }.returns(mockk(relaxed = true))
        every { services.close() } just Runs
        Services.set(services)
        return services
    }
}
