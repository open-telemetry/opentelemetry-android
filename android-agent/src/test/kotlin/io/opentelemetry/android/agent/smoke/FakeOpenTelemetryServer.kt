/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.smoke

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * Provides a fake server that can be used to test OTLP export. It launches a mock HTTP server
 * and provides functions that can wait for requests to be received and then deserializes them
 * into Protobuf models that can be asserted against.
 */
internal class FakeOpenTelemetryServer {
    private companion object {
        private const val REQUEST_TIMEOUT_SECS = 5L
    }

    private val server =
        MockWebServer().apply {
            start()
        }

    val url = server.url("/").toString()

    /**
     * Waits for a trace request or throws after a timeout.
     */
    fun awaitTraceRequest(): ExportTraceServiceRequest =
        awaitRequest {
            ExportTraceServiceRequest.parseFrom(it)
        }

    /**
     * Waits for a log request or throws after a timeout.
     */
    fun awaitLogRequest(): ExportLogsServiceRequest =
        awaitRequest {
            ExportLogsServiceRequest.parseFrom(it)
        }

    private fun <T> awaitRequest(deserializer: (stream: InputStream) -> T): T {
        val request = server.takeRequest(REQUEST_TIMEOUT_SECS, TimeUnit.SECONDS)
        checkNotNull(request)
        return readRequestBodyAsStream(request).use(deserializer)
    }

    private fun readRequestBodyAsStream(request: RecordedRequest): InputStream {
        val bytes =
            checkNotNull(request.body?.toByteArray()) {
                "No bytes in request body"
            }
        return gunzip(bytes)
    }

    private fun gunzip(bytes: ByteArray): InputStream = GZIPInputStream(bytes.inputStream())
}
