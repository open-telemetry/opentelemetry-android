/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.smoketest

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.smoketestapp.OTLP_ENDPOINT_EXTRA
import io.opentelemetry.android.smoketestapp.SMOKE_TEST_SCOPE_NAME
import io.opentelemetry.android.smoketestapp.SMOKE_TEST_SPAN_NAME
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.common.v1.KeyValue
import io.opentelemetry.proto.trace.v1.ResourceSpans
import io.opentelemetry.proto.trace.v1.ScopeSpans
import io.opentelemetry.proto.trace.v1.Span
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

@RunWith(AndroidJUnit4::class)
class MinifiedAppSmokeTest {
    @Test
    fun appLaunchesAndExportsTrace() {
        OtlpHttpServer().use { server ->
            val intent =
                Intent()
                    .setClassName(TARGET_PACKAGE, TARGET_ACTIVITY)
                    .putExtra(OTLP_ENDPOINT_EXTRA, server.url)

            ActivityScenario.launch<Activity>(intent).use {
                awaitExpectedTrace(server)
            }
        }
    }

    private fun awaitExpectedTrace(server: OtlpHttpServer) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(EXPORT_TIMEOUT_SECONDS)
        val receivedTargets = mutableListOf<String>()
        val receivedSpanNames = mutableListOf<String>()

        while (System.nanoTime() < deadline) {
            val request =
                server.takeRequest(deadline - System.nanoTime()) ?: break
            receivedTargets.add("${request.method} ${request.target}")
            if (request.target != TRACE_PATH) {
                continue
            }

            val exportRequest = parseTraceRequest(request)
            exportRequest.resourceSpansList.forEach { resourceSpans ->
                resourceSpans.scopeSpansList.forEach { scopeSpans ->
                    scopeSpans.spansList.forEach { span ->
                        receivedSpanNames.add(span.name)
                        if (span.name == SMOKE_TEST_SPAN_NAME) {
                            assertExpectedTrace(request, resourceSpans, scopeSpans, span)
                            return
                        }
                    }
                }
            }
        }

        throw AssertionError(
            "Timed out waiting for span '$SMOKE_TEST_SPAN_NAME'. " +
                "Requests: $receivedTargets; spans: $receivedSpanNames",
        )
    }

    private fun assertExpectedTrace(
        request: CapturedHttpRequest,
        resourceSpans: ResourceSpans,
        scopeSpans: ScopeSpans,
        span: Span,
    ) {
        assertSmoke(request.method == "POST") { "Expected POST but received ${request.method}" }
        assertSmoke(request.headers["content-type"]?.substringBefore(';') == PROTOBUF_CONTENT_TYPE) {
            "Expected $PROTOBUF_CONTENT_TYPE but received ${request.headers["content-type"]}"
        }
        assertSmoke(request.headers["content-encoding"] == "gzip") {
            "Expected gzip content encoding but received ${request.headers["content-encoding"]}"
        }
        assertSmoke(scopeSpans.scope.name == SMOKE_TEST_SCOPE_NAME) {
            "Expected scope '$SMOKE_TEST_SCOPE_NAME' but received '${scopeSpans.scope.name}'"
        }

        val resourceAttributes = resourceSpans.resource.attributesList.stringValues()
        EXPECTED_RESOURCE_ATTRIBUTES.forEach { (key, value) ->
            assertSmoke(resourceAttributes[key] == value) {
                "Expected $key=$value resource attribute: $resourceAttributes"
            }
        }
        listOf("telemetry.sdk.version", "device.model.name", "device.model.identifier").forEach { key ->
            assertSmoke(!resourceAttributes[key].isNullOrBlank()) {
                "Missing $key resource attribute: $resourceAttributes"
            }
        }
        val spanAttributes = span.attributesList.stringValues()
        assertSmoke(!spanAttributes["session.id"].isNullOrBlank()) {
            "Expected a non-empty session.id span attribute: $spanAttributes"
        }
        assertValidId("trace", span.traceId.toByteArray(), TRACE_ID_BYTES)
        assertValidId("span", span.spanId.toByteArray(), SPAN_ID_BYTES)
    }

    private fun parseTraceRequest(request: CapturedHttpRequest): ExportTraceServiceRequest {
        val inputStream: InputStream =
            when (val encoding = request.headers["content-encoding"]) {
                null, "identity" -> request.body.inputStream()
                "gzip" -> GZIPInputStream(request.body.inputStream())
                else -> throw AssertionError("Unsupported OTLP content encoding: $encoding")
            }
        return inputStream.use(ExportTraceServiceRequest::parseFrom)
    }

    private fun List<KeyValue>.stringValues(): Map<String, String> = associate { it.key to it.value.stringValue }

    private fun assertValidId(
        name: String,
        value: ByteArray,
        expectedSize: Int,
    ) {
        assertSmoke(value.size == expectedSize && value.any { it != 0.toByte() }) {
            "Expected a valid $name ID but received ${value.joinToString("") { "%02x".format(it) }}"
        }
    }

    private fun assertSmoke(
        condition: Boolean,
        message: () -> String,
    ) {
        if (!condition) {
            throw AssertionError(message())
        }
    }

    private companion object {
        const val TARGET_PACKAGE = "io.opentelemetry.android.smoketestapp"
        const val TARGET_ACTIVITY = "$TARGET_PACKAGE.SmokeTestActivity"
        const val TRACE_PATH = "/v1/traces"
        const val PROTOBUF_CONTENT_TYPE = "application/x-protobuf"
        const val SERVICE_NAME = "minified-smoke-test"
        const val EXPORT_TIMEOUT_SECONDS = 15L
        const val TRACE_ID_BYTES = 16
        const val SPAN_ID_BYTES = 8
        val EXPECTED_RESOURCE_ATTRIBUTES =
            mapOf(
                "service.name" to SERVICE_NAME,
                "telemetry.sdk.name" to "opentelemetry",
                "telemetry.sdk.language" to "java",
                "os.type" to "linux",
            )
    }
}
