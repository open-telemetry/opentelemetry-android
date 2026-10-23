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
    fun `install configures interceptors and uninstall resets them for existing and new clients`() {
        val instrumentation = OkHttpInstrumentation()
        val context = mockk<Context>()
        val openTelemetryRum =
            mockk<OpenTelemetryRum> {
                every { openTelemetry } returns otelTesting.openTelemetry
            }

        instrumentation.install(context, openTelemetryRum)

        // Existing client configured while installed
        val client =
            okhttp3.OkHttpClient
                .Builder()
                .addNetworkInterceptor(OkHttpSingletons.tracingInterceptor)
                .build()

        val request =
            okhttp3.Request
                .Builder()
                .url("http://localhost/")
                .build()

        // Before uninstall, tracing interceptor is active
        val chainBefore = mockk<okhttp3.Interceptor.Chain>()
        val response =
            okhttp3.Response
                .Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        every { chainBefore.request() } returns request
        every { chainBefore.proceed(any()) } returns response
        every { chainBefore.connection() } returns null

        client.networkInterceptors.first().intercept(chainBefore)
        assertThat(otelTesting.spans).hasSize(1)

        otelTesting.clearSpans()

        // Uninstall now
        instrumentation.uninstall(context, openTelemetryRum)

        // The SAME existing client executes a request after uninstall
        val chainAfter = mockk<okhttp3.Interceptor.Chain>()
        every { chainAfter.request() } returns request
        every { chainAfter.proceed(request) } returns response

        val result = client.networkInterceptors.first().intercept(chainAfter)
        assertThat(result).isSameAs(response)
        // No new spans should be created by the existing client
        assertThat(otelTesting.spans).isEmpty()
    }

    @Test
    fun `uninstall before install is safe noop`() {
        val instrumentation = OkHttpInstrumentation()
        val context = mockk<Context>()
        val openTelemetryRum = mockk<OpenTelemetryRum>()

        instrumentation.uninstall(context, openTelemetryRum)

        val request =
            okhttp3.Request
                .Builder()
                .url("http://localhost/")
                .build()
        val response =
            okhttp3.Response
                .Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        val chain = mockk<okhttp3.Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        val result = OkHttpSingletons.tracingInterceptor.intercept(chain)
        assertThat(result).isSameAs(response)
        assertThat(otelTesting.spans).isEmpty()
    }
}
