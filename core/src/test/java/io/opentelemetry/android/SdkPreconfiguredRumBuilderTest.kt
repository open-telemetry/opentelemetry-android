/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoaderImpl
import io.opentelemetry.android.instrumentation.OrderedAndroidInstrumentation
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.Clock.getDefault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class SdkPreconfiguredRumBuilderTest {
    @Test
    fun `ordered instrumentations are sorted according to constraints`() {
        val context = mockk<Context>()
        val sdk = mockk<OpenTelemetrySdk>()
        val config = mockk<OtelRumConfig>()
        val fooInstrumentation = mockk<AndroidInstrumentation>()
        val sessionInstrumentation = mockk<OrderedAndroidInstrumentation>(relaxed = true)
        val nativeCrashInstrumentation = mockk<OrderedAndroidInstrumentation>(relaxed = true)

        every { config.shouldDiscoverInstrumentations() } returns false // irrelevant
        every { config.isSuppressed(any()) } returns false
        every { fooInstrumentation.name } returns "foo"
        every { sessionInstrumentation.name } returns "session"
        every { sessionInstrumentation.installBefore } returns setOf("native-crash")
        every { sessionInstrumentation.installAfter } returns emptySet()
        every { nativeCrashInstrumentation.name } returns "native-crash"
        every { nativeCrashInstrumentation.installBefore } returns emptySet()
        every { nativeCrashInstrumentation.installAfter } returns setOf("session")

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
        builder.addInstrumentation(nativeCrashInstrumentation)
        builder.addInstrumentation(sessionInstrumentation)

        val result = builder.getEnabledInstrumentations()

        assertThat(result)
            .containsExactly(
                fooInstrumentation,
                sessionInstrumentation,
                nativeCrashInstrumentation,
            )
    }

    @Test
    fun `native crash instrumentation installs without session when session is not available`() {
        val context = mockk<Context>()
        val sdk = mockk<OpenTelemetrySdk>()
        val config = mockk<OtelRumConfig>()
        val fooInstrumentation = mockk<AndroidInstrumentation>()
        val nativeCrashInstrumentation = mockk<OrderedAndroidInstrumentation>(relaxed = true)

        every { config.shouldDiscoverInstrumentations() } returns false // irrelevant
        every { config.isSuppressed(any()) } returns false
        every { fooInstrumentation.name } returns "foo"
        every { nativeCrashInstrumentation.name } returns "native-crash"
        every { nativeCrashInstrumentation.installAfter } returns setOf("session")
        every { nativeCrashInstrumentation.installBefore } returns emptySet()

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

        assertThat(result).containsExactly(fooInstrumentation, nativeCrashInstrumentation)
    }

    @Test
    fun `cycle in instrumentation ordering logs warning and falls back to discovery order`() {
        val context = mockk<Context>()
        val sdk = mockk<OpenTelemetrySdk>()
        val config = mockk<OtelRumConfig>()
        val a = mockk<OrderedAndroidInstrumentation>(relaxed = true)
        val b = mockk<OrderedAndroidInstrumentation>(relaxed = true)

        every { config.shouldDiscoverInstrumentations() } returns false
        every { config.isSuppressed(any()) } returns false
        every { a.name } returns "inst-a"
        every { a.installAfter } returns setOf("inst-b")
        every { a.installBefore } returns emptySet()
        every { b.name } returns "inst-b"
        every { b.installAfter } returns setOf("inst-a")
        every { b.installBefore } returns emptySet()

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
        builder.addInstrumentation(a)
        builder.addInstrumentation(b)

        val result = builder.getEnabledInstrumentations()

        assertThat(result).containsExactlyInAnyOrder(a, b)
    }
}
