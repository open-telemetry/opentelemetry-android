/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.instrumentation.library.okhttp.internal.OkHttpSingletons
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.ConnectionErrorSpanInterceptor
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.TracingInterceptor
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class OkHttpInstrumentationTest {
    companion object {
        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    @AfterEach
    fun tearDown() {
        OkHttpSingletons.reset()
    }

    @Test
    fun validateDefaultHttpMethods() {
        val instrumentation = OkHttpInstrumentation()
        assertThat(instrumentation.knownMethods)
            .containsExactlyInAnyOrder(
                "CONNECT",
                "DELETE",
                "GET",
                "HEAD",
                "OPTIONS",
                "PATCH",
                "POST",
                "PUT",
                "TRACE",
            )
    }

    @Test
    fun `install configures interceptors and uninstall resets them`() {
        val instrumentation = OkHttpInstrumentation()
        val context = mockk<Context>()
        val openTelemetryRum =
            mockk<OpenTelemetryRum> {
                every { openTelemetry } returns otelTesting.openTelemetry
            }

        instrumentation.install(context, openTelemetryRum)

        assertThat(OkHttpSingletons.connectionErrorInterceptor).isInstanceOf(ConnectionErrorSpanInterceptor::class.java)
        assertThat(OkHttpSingletons.tracingInterceptor).isInstanceOf(TracingInterceptor::class.java)

        instrumentation.uninstall(context, openTelemetryRum)

        assertThat(OkHttpSingletons.connectionErrorInterceptor).isNotInstanceOf(ConnectionErrorSpanInterceptor::class.java)
        assertThat(OkHttpSingletons.tracingInterceptor).isNotInstanceOf(TracingInterceptor::class.java)
    }

    @Test
    fun `uninstall before install is safe noop`() {
        val instrumentation = OkHttpInstrumentation()
        val context = mockk<Context>()
        val openTelemetryRum = mockk<OpenTelemetryRum>()

        instrumentation.uninstall(context, openTelemetryRum)

        assertThat(OkHttpSingletons.connectionErrorInterceptor).isNotInstanceOf(ConnectionErrorSpanInterceptor::class.java)
        assertThat(OkHttpSingletons.tracingInterceptor).isNotInstanceOf(TracingInterceptor::class.java)
    }
}
