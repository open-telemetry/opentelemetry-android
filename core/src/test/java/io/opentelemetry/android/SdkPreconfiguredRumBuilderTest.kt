/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoaderImpl
import io.opentelemetry.android.internal.services.ServicesAwareInstrumentation
import io.opentelemetry.android.internal.services.ServicesFactory
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.Clock.getDefault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class SdkPreconfiguredRumBuilderTest {
    @Test
    fun `shared services are injected before a discovered instrumentation is installed`() {
        verifyServicesInjection(discovered = true)
    }

    @Test
    fun `shared services are injected before a manually added instrumentation is installed`() {
        verifyServicesInjection(discovered = false)
    }

    private fun verifyServicesInjection(discovered: Boolean) {
        val services = mockk<ServicesFactory>()
        val instrumentation = mockk<ServicesUsingInstrumentation>(relaxed = true)
        every { instrumentation.name } returns "services-using"
        val loader = AndroidInstrumentationLoaderImpl()
        val config = OtelRumConfig()
        if (discovered) {
            loader.registerForTest(instrumentation)
        } else {
            config.disableInstrumentationDiscovery()
        }
        val sdk = mockk<OpenTelemetrySdk>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        try {
            val rum =
                SdkPreconfiguredRumBuilder(
                    context,
                    sdk,
                    SessionProvider { "session" },
                    config,
                    getDefault(),
                    loader,
                    services,
                ).apply {
                    if (!discovered) {
                        addInstrumentation(instrumentation)
                    }
                }.build()

            verifyOrder {
                instrumentation.configureServices(services)
                instrumentation.install(context, rum)
            }

            rum.shutdown()
            verify { instrumentation.uninstall(context, rum) }
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }
    }

    @Test
    fun `suppressed instrumentation does not receive services`() {
        val instrumentation = mockk<ServicesUsingInstrumentation>(relaxed = true)
        every { instrumentation.name } returns "suppressed"
        val config = OtelRumConfig().disableInstrumentationDiscovery().suppressInstrumentation("suppressed")
        val sdk = mockk<OpenTelemetrySdk>(relaxed = true)

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        try {
            val rum =
                SdkPreconfiguredRumBuilder(
                    mockk<Context>(relaxed = true),
                    sdk,
                    SessionProvider { "session" },
                    config,
                    getDefault(),
                    AndroidInstrumentationLoaderImpl(),
                    mockk<ServicesFactory>(),
                ).addInstrumentation(instrumentation)
                    .build()

            verify(exactly = 0) {
                instrumentation.configureServices(any())
                instrumentation.install(any(), any())
            }
            rum.shutdown()
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }
    }

    @Test
    fun `a failing services hook does not stop other instrumentations from installing`() {
        val failing = mockk<ServicesUsingInstrumentation>(relaxed = true)
        val working = mockk<AndroidInstrumentation>(relaxed = true)
        every { failing.name } returns "failing"
        every { working.name } returns "working"
        every { failing.configureServices(any()) } throws IllegalStateException("configuration failed")
        val config = OtelRumConfig().disableInstrumentationDiscovery()
        val sdk = mockk<OpenTelemetrySdk>(relaxed = true)

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        try {
            val rum =
                SdkPreconfiguredRumBuilder(
                    mockk<Context>(relaxed = true),
                    sdk,
                    SessionProvider { "session" },
                    config,
                    getDefault(),
                    AndroidInstrumentationLoaderImpl(),
                    mockk<ServicesFactory>(),
                ).addInstrumentation(failing)
                    .addInstrumentation(working)
                    .build()

            verify(exactly = 0) { failing.install(any(), any()) }
            verify { working.install(any(), rum) }
            rum.shutdown()
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }
    }

    private interface ServicesUsingInstrumentation :
        AndroidInstrumentation,
        ServicesAwareInstrumentation

    @Test
    fun `session and native crash instrumentations must come first`() {
        val context = mockk<Context>()
        val sdk = mockk<OpenTelemetrySdk>()
        val config = mockk<OtelRumConfig>()
        val fooInstrumentation = mockk<AndroidInstrumentation>()
        val sessionInstrumentation = mockk<AndroidInstrumentation>()
        val nativeCrashInstrumentation = mockk<AndroidInstrumentation>()

        every { config.shouldDiscoverInstrumentations() } returns false // irrelevant
        every { config.isSuppressed(any()) } returns false
        every { fooInstrumentation.name } returns "foo"
        every { sessionInstrumentation.name } returns "session"
        every { nativeCrashInstrumentation.name } returns "native-crash"

        val sessionProvider =
            object : SessionProvider {
                override fun getSessionId(): String {
                    fail("Should not have been called!")
                }
            }

        val builder =
            SdkPreconfiguredRumBuilder(
                context,
                sdk,
                sessionProvider,
                config,
                getDefault(),
                AndroidInstrumentationLoaderImpl(),
                mockk<ServicesFactory>(),
            )
        builder.addInstrumentation(fooInstrumentation)
        builder.addInstrumentation(sessionInstrumentation)
        builder.addInstrumentation(nativeCrashInstrumentation)

        val result = builder.getEnabledInstrumentations()

        assertThat(result)
            .containsExactly(
                sessionInstrumentation,
                nativeCrashInstrumentation,
                fooInstrumentation,
            )
    }

    @Test
    fun `native crash instrumentation comes first when session instrumentation is unavailable`() {
        val context = mockk<Context>()
        val sdk = mockk<OpenTelemetrySdk>()
        val config = mockk<OtelRumConfig>()
        val fooInstrumentation = mockk<AndroidInstrumentation>()
        val nativeCrashInstrumentation = mockk<AndroidInstrumentation>()

        every { config.shouldDiscoverInstrumentations() } returns false // irrelevant
        every { config.isSuppressed(any()) } returns false
        every { fooInstrumentation.name } returns "foo"
        every { nativeCrashInstrumentation.name } returns "native-crash"

        val sessionProvider = SessionProvider { fail("Should not have been called!") }
        val builder =
            SdkPreconfiguredRumBuilder(
                context,
                sdk,
                sessionProvider,
                config,
                getDefault(),
                AndroidInstrumentationLoaderImpl(),
                mockk<ServicesFactory>(),
            )
        builder.addInstrumentation(fooInstrumentation)
        builder.addInstrumentation(nativeCrashInstrumentation)

        val result = builder.getEnabledInstrumentations()

        assertThat(result).containsExactly(nativeCrashInstrumentation, fooInstrumentation)
    }

    @Test
    fun `a failing instrumentation does not stop the others from installing or uninstalling`() {
        val sdk = mockk<OpenTelemetrySdk>(relaxed = true)
        val config = mockk<OtelRumConfig>(relaxed = true)
        val failing = mockk<AndroidInstrumentation>(relaxed = true)
        val working = mockk<AndroidInstrumentation>(relaxed = true)

        every { config.isSuppressed(any()) } returns false
        every { failing.name } returns "failing"
        every { working.name } returns "working"
        every { failing.install(any(), any()) } throws IllegalStateException("install failed")
        every { failing.uninstall(any(), any()) } throws IllegalStateException("uninstall failed")

        // build() installs a crash flush handler globally; restore the previous one afterwards.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        try {
            val rum =
                SdkPreconfiguredRumBuilder(
                    mockk<Context>(relaxed = true),
                    sdk,
                    SessionProvider { "session" },
                    config,
                    getDefault(),
                    AndroidInstrumentationLoaderImpl(),
                    mockk<ServicesFactory>(),
                ).addInstrumentation(failing)
                    .addInstrumentation(working)
                    .build()

            verify { working.install(any(), any()) }

            rum.shutdown()

            verify { working.uninstall(any(), any()) }
            verify { sdk.shutdown() }
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }
    }
}
