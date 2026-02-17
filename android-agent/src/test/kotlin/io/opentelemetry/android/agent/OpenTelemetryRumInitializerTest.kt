/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.connectivity.ExportProtocol
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
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
    fun `Verify timeoutHandler initialization with HTTP export`() {
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

    @Test
    fun `Verify initialization with gRPC export`() {
        createAndSetServiceManager()

        OpenTelemetryRumInitializer.initialize(
            context = RuntimeEnvironment.getApplication(),
            configuration = {
                grpcExport {
                    endpoint = "http://127.0.0.1:4317"
                }
            },
        )

        verify {
            appLifecycle.registerListener(any<SessionIdTimeoutHandler>())
        }
    }

    @Test
    fun `Verify initialization with unified export using HTTP`() {
        createAndSetServiceManager()

        OpenTelemetryRumInitializer.initialize(
            context = RuntimeEnvironment.getApplication(),
            configuration = {
                export {
                    protocol = ExportProtocol.HTTP
                    endpoint = "http://127.0.0.1:4318"
                }
            },
        )

        verify {
            appLifecycle.registerListener(any<SessionIdTimeoutHandler>())
        }
    }

    @Test
    fun `Verify initialization with unified export using GRPC`() {
        createAndSetServiceManager()

        OpenTelemetryRumInitializer.initialize(
            context = RuntimeEnvironment.getApplication(),
            configuration = {
                export {
                    protocol = ExportProtocol.GRPC
                    endpoint = "http://127.0.0.1:4317"
                }
            },
        )

        verify {
            appLifecycle.registerListener(any<SessionIdTimeoutHandler>())
        }
    }

    @Test
    fun `Verify initialization with default configuration`() {
        createAndSetServiceManager()

        OpenTelemetryRumInitializer.initialize(
            context = RuntimeEnvironment.getApplication(),
        )

        verify {
            appLifecycle.registerListener(any<SessionIdTimeoutHandler>())
        }
    }

    @Test
    fun `Verify initialization with application context`() {
        createAndSetServiceManager()

        OpenTelemetryRumInitializer.initialize(
            context = RuntimeEnvironment.getApplication().applicationContext,
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

    @Test
    fun `Verify initialization with NONE compression for HTTP`() {
        createAndSetServiceManager()

        OpenTelemetryRumInitializer.initialize(
            context = RuntimeEnvironment.getApplication(),
            configuration = {
                httpExport {
                    baseUrl = "http://127.0.0.1:4318"
                    compression = io.opentelemetry.android.agent.connectivity.Compression.NONE
                }
            },
        )

        verify {
            appLifecycle.registerListener(any<SessionIdTimeoutHandler>())
        }
    }

    @Test
    fun `Verify initialization with NONE compression for gRPC`() {
        createAndSetServiceManager()

        OpenTelemetryRumInitializer.initialize(
            context = RuntimeEnvironment.getApplication(),
            configuration = {
                grpcExport {
                    endpoint = "http://127.0.0.1:4317"
                    compression = io.opentelemetry.android.agent.connectivity.Compression.NONE
                }
            },
        )

        verify {
            appLifecycle.registerListener(any<SessionIdTimeoutHandler>())
        }
    }

    @Test
    fun `Verify initialization with unified export and NONE compression`() {
        createAndSetServiceManager()

        OpenTelemetryRumInitializer.initialize(
            context = RuntimeEnvironment.getApplication(),
            configuration = {
                export {
                    protocol = ExportProtocol.GRPC
                    endpoint = "http://127.0.0.1:4317"
                    compression = io.opentelemetry.android.agent.connectivity.Compression.NONE
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
