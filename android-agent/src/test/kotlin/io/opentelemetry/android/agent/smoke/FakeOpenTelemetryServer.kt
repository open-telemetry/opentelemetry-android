/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.smoke

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import java.io.InputStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.zip.GZIPInputStream

/**
 * Provides a fake server that can be used to test OTLP export. It launches a mock HTTP server
 * and provides functions that can wait for requests to be received and then deserializes them
 * into Protobuf models that can be asserted against.
 */
internal class FakeOpenTelemetryServer {
    private val logRequests: MutableCollection<ExportLogsServiceRequest> = ConcurrentLinkedQueue()
    private val traceRequests: MutableCollection<ExportTraceServiceRequest> =
        ConcurrentLinkedQueue()

    private val requestHandler: Dispatcher =
        object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                when (request.target) {
                    "/v1/traces" -> {
                        traceRequests.add(
                            readRequestBodyAsStream(request).use {
                                ExportTraceServiceRequest.parseFrom(it)
                            },
                        )
                    }

                    "/v1/logs" -> {
                        logRequests.add(
                            readRequestBodyAsStream(request).use {
                                ExportLogsServiceRequest.parseFrom(it)
                            },
                        )
                    }

                    else -> error("Unsupported request path: ${request.target}")
                }
                return MockResponse(200)
            }
        }

    private val server =
        MockWebServer().apply {
            dispatcher = requestHandler
            start()
        }

    val url = server.url("/").toString()

    /**
     * Waits for a trace request or throws after a timeout.
     */
    fun awaitTraceRequest(predicate: (ExportTraceServiceRequest) -> Boolean): ExportTraceServiceRequest =
        awaitRequestMatchingPredicate(traceRequests, predicate)

    /**
     * Waits for a log request or throws after a timeout.
     */
    fun awaitLogRequest(predicate: (ExportLogsServiceRequest) -> Boolean): ExportLogsServiceRequest =
        awaitRequestMatchingPredicate(logRequests, predicate)

    private fun readRequestBodyAsStream(request: RecordedRequest): InputStream {
        val bytes =
            checkNotNull(request.body?.toByteArray()) {
                "No bytes in request body"
            }
        return gunzip(bytes)
    }

    private fun gunzip(bytes: ByteArray): InputStream = GZIPInputStream(bytes.inputStream())

    private fun <T> awaitRequestMatchingPredicate(
        collection: MutableCollection<T>,
        predicate: (T) -> Boolean,
        waitTimeMs: Int = 5000,
        checkIntervalMs: Int = 1,
    ): T {
        val tries: Int = waitTimeMs / checkIntervalMs
        val countDownLatch = CountDownLatch(1)

        repeat(tries) {
            val request = collection.find(predicate)
            if (request != null) {
                collection.remove(request)
                return request
            } else {
                countDownLatch.await(checkIntervalMs.toLong(), TimeUnit.MILLISECONDS)
            }
        }
        throw TimeoutException(
            "Timed out waiting for HTTP request. " +
                "Received ${logRequests.size} log requests and ${traceRequests.size} trace requests.",
        )
    }
}
