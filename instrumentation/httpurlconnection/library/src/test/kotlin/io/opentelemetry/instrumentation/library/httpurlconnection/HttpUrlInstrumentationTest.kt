/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.instrumentation.library.httpurlconnection.internal.HttpUrlConnectionSingletons
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class HttpUrlInstrumentationTest {
    companion object {
        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    @AfterEach
    fun tearDown() {
        HttpUrlConnectionSingletons.reset()
    }

    @Test
    fun `install configures singletons and uninstall resets them`() {
        val instrumentation = HttpUrlInstrumentation()
        val context = mockk<Context>()
        val rum =
            mockk<OpenTelemetryRum> {
                every { openTelemetry } returns otelTesting.openTelemetry
            }

        instrumentation.install(context, rum)

        assertThat(HttpUrlConnectionSingletons.openTelemetryInstance()).isSameAs(otelTesting.openTelemetry)
        assertThat(HttpUrlConnectionSingletons.instrumenter()).isNotNull

        instrumentation.uninstall(context, rum)

        assertThat(HttpUrlConnectionSingletons.openTelemetryInstance()).isNotSameAs(otelTesting.openTelemetry)
        assertThat(HttpUrlConnectionSingletons.instrumenter()).isNotNull
    }

    @Test
    fun `uninstall before install is safe noop`() {
        val instrumentation = HttpUrlInstrumentation()
        val context = mockk<Context>()
        val rum = mockk<OpenTelemetryRum>()

        instrumentation.uninstall(context, rum)

        assertThat(HttpUrlConnectionSingletons.openTelemetryInstance()).isNotNull
        assertThat(HttpUrlConnectionSingletons.instrumenter()).isNotNull
    }
}
