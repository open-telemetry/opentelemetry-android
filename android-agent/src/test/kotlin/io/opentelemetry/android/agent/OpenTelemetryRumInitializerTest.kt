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
        val rum = OpenTelemetryRumInitializer.initialize(
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

        val rum = OpenTelemetryRumInitializer.initialize(
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

    private fun createAndSetServiceManager(): Services {
        val services = mockk<Services>()
        every { services.appLifecycle }.returns(appLifecycle)
        every { services.currentNetworkProvider }.returns(mockk(relaxed = true))
        every { services.visibleScreenTracker }.returns(mockk(relaxed = true))
        every { services.cacheStorage }.returns(mockk(relaxed = true))
        every { services.close() } just Runs
        Services.set(services)
        return services
    }
}
