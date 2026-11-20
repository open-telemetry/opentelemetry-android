/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

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
