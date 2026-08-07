/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.smoketest

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

@RunWith(AndroidJUnit4::class)
class MinifiedAppSmokeTest {
    private var server: MockWebServer? = null

    @Before
    fun setUp() {
        server =
            MockWebServer().apply {
                dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse =
                            if (request.target == "/v1/traces") {
                                MockResponse(200)
                            } else {
                                MockResponse(404)
                            }
                    }
                start()
            }
    }

    @After
    fun tearDown() {
        server?.close()
    }

    @Test
    fun appLaunchesAndExportsTrace() {
        val server = checkNotNull(server)
        val intent =
            Intent()
                .setClassName(TARGET_PACKAGE, TARGET_ACTIVITY)
                .putExtra(OTLP_ENDPOINT_EXTRA, server.url("/").toString())

        ActivityScenario.launch<Activity>(intent).use {
            awaitTraceRequest(server, SMOKE_TEST_SPAN_NAME)
        }
    }

    private fun awaitTraceRequest(
        server: MockWebServer,
        spanName: String,
    ) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        val receivedSpanNames = mutableListOf<String>()

        while (System.nanoTime() < deadline) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) {
                break
            }
            val recordedRequest = server.takeRequest(remaining, TimeUnit.NANOSECONDS) ?: break
            check(recordedRequest.target == "/v1/traces") {
                "Expected an OTLP trace request but received ${recordedRequest.target}"
            }

            val exportRequest = parseTraceRequest(recordedRequest)
            val spanNames =
                exportRequest.resourceSpansList
                    .flatMap { it.scopeSpansList }
                    .flatMap { it.spansList }
                    .map { it.name }
            receivedSpanNames.addAll(spanNames)
            if (spanName in spanNames) {
                return
            }
        }

        throw AssertionError(
            "Timed out waiting for span '$spanName'. Received spans: $receivedSpanNames",
        )
    }

    private fun parseTraceRequest(recordedRequest: RecordedRequest): ExportTraceServiceRequest {
        val requestBody =
            checkNotNull(recordedRequest.body) {
                "OTLP trace request did not contain a body"
            }.toByteArray()
        val inputStream: InputStream =
            when (val encoding = recordedRequest.headers["Content-Encoding"]) {
                null, "identity" -> requestBody.inputStream()
                "gzip" -> GZIPInputStream(requestBody.inputStream())
                else -> error("Unsupported OTLP content encoding: $encoding")
            }

        return inputStream.use(ExportTraceServiceRequest::parseFrom)
    }

    private companion object {
        const val TARGET_PACKAGE = "io.opentelemetry.android.smoketestapp"
        const val TARGET_ACTIVITY = "$TARGET_PACKAGE.SmokeTestActivity"
        const val OTLP_ENDPOINT_EXTRA = "io.opentelemetry.android.smoketest.OTLP_ENDPOINT"
        const val SMOKE_TEST_SPAN_NAME = "minified-app-smoke-test"
    }
}
