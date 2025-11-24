/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("ktlint:standard:package-name")

package io.opentelemetry.instrumentation.library.okhttp.v3_0

import io.opentelemetry.android.test.common.OpenTelemetryRumRule
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch

class InstrumentationTest {
    private lateinit var server: MockWebServer

    @get:Rule
    internal var openTelemetryRumRule: OpenTelemetryRumRule = OpenTelemetryRumRule()

    @Before
    @Throws(IOException::class)
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        server.close()
    }

    @Test
    @Throws(IOException::class)
    fun okhttpTraces() {
        server.enqueue(MockResponse.Builder().code(200).build())

        val lock = CountDownLatch(1)
        val span = openTelemetryRumRule.getSpan()

        span.makeCurrent().use { ignored ->
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor(
                        Interceptor { chain: Interceptor.Chain ->
                            val currentSpan = Span.current().spanContext
                            assertThat(span.spanContext.traceId)
                                .isEqualTo(currentSpan.traceId)
                            lock.countDown()
                            chain.proceed(chain.request())
                        },
                    ).build()
            createCall(client, "/test/").execute().close()
        }
        lock.await()
        span.end()

        assertThat(
            openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems.size
                .toLong(),
        ).isEqualTo(2)
    }

    @Test
    @Throws(InterruptedException::class)
    fun okhttpTraces_with_callback() {
        val lock = CountDownLatch(2)
        val span = openTelemetryRumRule.getSpan()

        span.makeCurrent().use { ignored ->
            server.enqueue(MockResponse.Builder().code(200).build())
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor(
                        Interceptor { chain: Interceptor.Chain ->
                            val currentSpan = Span.current().spanContext
                            // Verify context propagation.
                            assertThat(span.spanContext.traceId)
                                .isEqualTo(currentSpan.traceId)
                            lock.countDown()
                            chain.proceed(chain.request())
                        },
                    ).build()
            createCall(client, "/test/")
                .enqueue(
                    object : Callback {
                        override fun onFailure(
                            call: Call,
                            e: IOException,
                        ) {
                        }

                        override fun onResponse(
                            call: Call,
                            response: Response,
                        ) {
                            // Verify that the original caller's context is the current one
                            // here.
                            assertThat(span).isEqualTo(Span.current())
                            lock.countDown()
                        }
                    },
                )
        }
        lock.await()
        span.end()

        assertThat(
            openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems.size
                .toLong(),
        ).isEqualTo(2)
    }

    @Test
    @Throws(InterruptedException::class)
    fun avoidCreatingSpansForInternalOkhttpRequests() {
        // NOTE: For some reason this test always passes when running all the tests in this file at
        // once,
        // so it should be run isolated to actually get it to fail when it's expected to fail.
        val exporter =
            OtlpHttpSpanExporter.builder().setEndpoint(server.url("").toString()).build()
        val openTelemetry: OpenTelemetry =
            OpenTelemetrySdk
                .builder()
                .setTracerProvider(
                    SdkTracerProvider
                        .builder()
                        .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                        .build(),
                ).build()

        server.enqueue(MockResponse.Builder().code(200).build())

        // This span should trigger 1 export okhttp call, which is the only okhttp call expected
        // for this test case.
        openTelemetry
            .tracerBuilder("Some Scope")
            .build()
            .spanBuilder("Some Span")
            .startSpan()
            .end()

        // Wait for unwanted extra okhttp requests.
        var loop = 0
        while (loop < 10) {
            Thread.sleep(100)
            // Stop waiting if we get at least one unwanted request.
            if (server.requestCount > 1) {
                break
            }
            loop++
        }

        assertThat(server.requestCount.toLong()).isEqualTo(1)
    }

    private fun createCall(
        client: OkHttpClient,
        urlPath: String,
    ): Call {
        val request = Request.Builder().url(server.url(urlPath)).build()
        return client.newCall(request)
    }
}
