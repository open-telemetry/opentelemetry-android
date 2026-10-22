/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.extensions.OtherTestApi
import io.opentelemetry.android.extensions.OtherTestApiProvider
import io.opentelemetry.android.extensions.TestApi
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
    fun `extensions are snapshotted before installation without instrumentation discovery`() {
        val context = mockk<Application>()
        val sdk = OpenTelemetrySdk.builder().build()
        val instrumentation = mockk<AndroidInstrumentation>(relaxed = true)
        var expected: TestApi = object : TestApi {}
        every { instrumentation.name } returns "extension-test"
        every { instrumentation.install(any(), any()) } answers {
            val rum = secondArg<OpenTelemetryRum>()
            assertThat(rum.getExtension(TestApi::class.java)).isSameAs(expected)
            assertThat(rum.getExtension(OtherTestApi::class.java)).isInstanceOf(OtherTestApiProvider::class.java)
        }
        val builder =
            SdkPreconfiguredRumBuilder(
                context,
                sdk,
                SessionProvider.getNoop(),
                OtelRumConfig().disableInstrumentationDiscovery(),
                getDefault(),
                mockk(),
            ).addInstrumentation(instrumentation)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        try {
            val first = builder.addApiExtension(expected).build()
            val original = expected
            expected = object : TestApi {}
            val second = builder.addApiExtension(expected).build()
            assertThat(first.getExtension(TestApi::class.java)).isSameAs(original)
            assertThat(second.getExtension(TestApi::class.java)).isSameAs(expected)
            second.shutdown()
        } finally {
            sdk.close()
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
