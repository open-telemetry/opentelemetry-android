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
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class SdkPreconfiguredRumBuilderTest {
    @Test
    fun `session instrumentation must come first`() {
        val context = mockk<Context>()
        val sdk = mockk<OpenTelemetrySdk>()
        val config = mockk<OtelRumConfig>()
        val fooInstrumentation = mockk<AndroidInstrumentation>()
        val sessionInstrumentation = mockk<AndroidInstrumentation>()

        every { config.shouldDiscoverInstrumentations() } returns false // irrelevant
        every { config.isSuppressed(any()) } returns false
        every { fooInstrumentation.name } returns "foo"
        every { sessionInstrumentation.name } returns "session"

        val sessionProvider =
            object : SessionProvider {
                override fun getSessionId(): String {
                    fail("Should not have been called!")
                }

                override fun getPreviousSessionId(): String {
                    fail("Should not have been called!")
                }
            }

        val builder = RumBuilder.builder(context, sdk, config, sessionProvider)
        builder.addInstrumentation(fooInstrumentation)
        builder.addInstrumentation(sessionInstrumentation)

        val result = builder.getEnabledInstrumentations()

        assertThat(result[0]).isSameAs(sessionInstrumentation)
    }
}
