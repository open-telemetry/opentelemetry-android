/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
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
    }

    @Test
    fun `Verify timeoutHandler initialization 2`() {
        createAndSetServiceManager()

        OpenTelemetryRumInitializer.initialize(
            context = RuntimeEnvironment.getApplication(),
            configuration = {
                httpExport {
                    baseUrl = "http://127.0.0.1:4318"
                }
            },
        )

        verify {
            appLifecycle.registerListener(any<SessionIdTimeoutHandler>())
        }
    }

    @After
    fun tearDown() {
        Services.set(null)
    }

    private fun createAndSetServiceManager(): Services {
        val services = mockk<Services>()
        every { services.appLifecycle }.returns(appLifecycle)
        every { services.currentNetworkProvider }.returns(mockk(relaxed = true))
        every { services.visibleScreenTracker }.returns(mockk(relaxed = true))
        Services.set(services)
        return services
    }
}
