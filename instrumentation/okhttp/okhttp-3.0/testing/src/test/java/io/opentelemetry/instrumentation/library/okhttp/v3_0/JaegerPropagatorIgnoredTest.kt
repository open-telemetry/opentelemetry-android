/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.instrumentation.library.okhttp.v3_0

import com.google.common.truth.Truth.assertThat
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.OkHttp3Singletons
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException


@RunWith(RobolectricTestRunner::class)
class JaegerPropagatorIgnoredTest {

    @Ignore("State conflict with case 1")
    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun `case 0 when jaeger propagator is not added it will not trigger the request with uber header`() {

        //arrange
        val inMemorySpanExporter = InMemorySpanExporter.create()
        GlobalOpenTelemetryUtil.setSdkWithAllDefault()
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setResponseCode(200))
        val span = SpanUtil.startSpan()
        //act
        span.makeCurrent().use {
            execute(span, server)
        }
        span.end()
        //affirm
        assertThat(inMemorySpanExporter.finishedSpanItems).hasSize(0)
        //assert
        val recordedRequest = server.takeRequest()
        val uberTraceId = recordedRequest.headers["uber-trace-id"]
        val spanTraceId = span.spanContext.traceId
        //example value 8d828d3c7c8663418b067492675bef12
        assertThat(spanTraceId).isNotEmpty()
        assertThat(uberTraceId).isNull()

        //clean up
        server.shutdown()
        inMemorySpanExporter.reset()
    }


    private fun execute(parentSpan: Span, server: MockWebServer) {
        val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(TestInjectingInterceptor())
                //Pay attention that this is done without any context related to open telemetry.
                .addNetworkInterceptor(OkHttp3Singletons.TRACING_INTERCEPTOR)
                .addInterceptor {
                    response(parentSpan, it)
                }
                .build()
        createCall(client, server).execute().close()
    }

    private fun response(parentSpan: Span, chain: Interceptor.Chain): Response {
        val currentSpanContext: SpanContext = Span.current().spanContext
        assertThat(currentSpanContext.traceId).isEqualTo(parentSpan.spanContext.traceId)
        return chain.proceed(chain.request())
    }

    private fun createCall(client: OkHttpClient, server: MockWebServer): Call {
        val request: Request = Request.Builder().url(server.url("/test/")).build()
        return client.newCall(request)
    }


}
