/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoaderImpl
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.Clock.getDefault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class SdkPreconfiguredRumBuilderTest {
    @Test
    fun `manual session replacement uses the supplied provider`() {
        val provider = mockk<SessionProvider>()
        every { provider.startNewSession() } returns true
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val rum =
            SdkPreconfiguredRumBuilder(
                mockk<Application>(),
                OpenTelemetrySdk.builder().build(),
                provider,
                OtelRumConfig().disableInstrumentationDiscovery(),
                getDefault(),
                AndroidInstrumentationLoaderImpl(),
            ).build()
        try {
            assertThat(rum.startNewSession()).isTrue()
            verify(exactly = 1) { provider.startNewSession() }
        } finally {
            rum.shutdown()
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }
    }

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
            )
        builder.addInstrumentation(fooInstrumentation)
        builder.addInstrumentation(nativeCrashInstrumentation)

        val result = builder.getEnabledInstrumentations()

        assertThat(result).containsExactly(nativeCrashInstrumentation, fooInstrumentation)
    }
}
