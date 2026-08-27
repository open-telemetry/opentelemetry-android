/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.smoketest

import io.opentelemetry.instrumentation.testing.internal.TelemetryConverter
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.testing.internal.armeria.client.WebClient
import io.opentelemetry.testing.internal.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.testing.internal.protobuf.util.JsonFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.time.Duration
import java.util.concurrent.TimeUnit

internal class TelemetryRetriever(
    backendPort: Int,
    private val telemetryTimeout: Duration,
) {
    private val client = WebClient.of("http://localhost:$backendPort")

    fun waitForTraces(): List<SpanData> {
        val requests =
            try {
                Json
                    .decodeFromString<List<JsonElement>>(waitForContent("get-traces"))
                    .map { requestJson ->
                        val builder = ExportTraceServiceRequest.newBuilder()
                        JsonFormat.parser().merge(requestJson.toString(), builder)
                        builder.build()
                    }
            } catch (failure: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException(failure)
            }

        return TelemetryConverter.getSpanData(requests.flatMap { it.resourceSpansList })
    }

    private fun waitForContent(path: String): String {
        var previousSize = 0
        val deadline = System.currentTimeMillis() + telemetryTimeout.toMillis()
        var content = "[]"
        while (System.currentTimeMillis() < deadline) {
            content =
                client
                    .get(path)
                    .aggregate()
                    .join()
                    .contentUtf8()
            if (content.length > 2 && content.length == previousSize) {
                break
            }

            previousSize = content.length
            TimeUnit.MILLISECONDS.sleep(500)
        }
        return content
    }
}
