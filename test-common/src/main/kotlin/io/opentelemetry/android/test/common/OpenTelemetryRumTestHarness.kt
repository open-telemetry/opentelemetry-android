/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.test.common

import android.content.Context
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.RumBuilder
import io.opentelemetry.api.common.AttributeType
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.json.JSONArray
import org.json.JSONObject
import org.junit.runner.Description

internal const val COLLECT_TELEMETRY_DOCS_PROPERTY = "collectTelemetryDocs"
internal const val TELEMETRY_DOCS_OUTPUT_DIRECTORY_PROPERTY = "otel.telemetryDocs.outputDirectory"

internal class OpenTelemetryRumTestHarness {
    lateinit var openTelemetryRum: OpenTelemetryRum
        private set
    lateinit var inMemorySpanExporter: InMemorySpanExporter
        private set
    lateinit var inMemoryLogExporter: InMemoryLogRecordExporter
        private set

    fun setUp(context: Context) {
        inMemorySpanExporter = InMemorySpanExporter.create()
        inMemoryLogExporter = InMemoryLogRecordExporter.create()
        openTelemetryRum =
            RumBuilder
                .builder(context)
                .addLoggerProviderCustomizer { logger, _ ->
                    logger.addLogRecordProcessor(
                        SimpleLogRecordProcessor.create(inMemoryLogExporter),
                    )
                }.addTracerProviderCustomizer { tracer, _ ->
                    tracer.addSpanProcessor(SimpleSpanProcessor.create(inMemorySpanExporter))
                }.build()
    }

    fun captureJson(): String {
        val signals = captureSpans() + captureLogs()
        val jsonSignals = JSONArray()
        signals
            .sortedWith(compareBy(ObservedSignal::type, ObservedSignal::name, ObservedSignal::scope))
            .forEach { jsonSignals.put(it.toJson()) }
        return JSONObject().put("signals", jsonSignals).toString()
    }

    private fun captureSpans(): List<ObservedSignal> =
        inMemorySpanExporter.finishedSpanItems.map { span ->
            ObservedSignal(
                type = "span",
                name = span.name,
                scope = span.instrumentationScopeInfo.name,
                attributes = span.attributes.observedAttributes(),
            )
        }

    private fun captureLogs(): List<ObservedSignal> =
        inMemoryLogExporter.finishedLogRecordItems.map { log ->
            ObservedSignal(
                type = if (log.eventName == null) "log" else "event",
                name = log.eventName ?: "log",
                scope = log.instrumentationScopeInfo.name,
                attributes = log.attributes.observedAttributes(),
            )
        }
}

private data class ObservedSignal(
    val type: String,
    val name: String,
    val scope: String,
    val attributes: List<ObservedAttribute>,
) {
    fun toJson(): JSONObject =
        JSONObject()
            .put("type", type)
            .put("name", name)
            .put("scope", scope)
            .apply {
                put(
                    "attributes",
                    JSONArray().apply {
                        attributes.forEach { put(it.toJson()) }
                    },
                )
            }
}

private data class ObservedAttribute(
    val name: String,
    val type: String,
    val value: Any,
) {
    fun toJson(): JSONObject =
        JSONObject()
            .put("name", name)
            .put("type", type)
            .put("value", value.toJsonValue())
}

private fun Attributes.observedAttributes(): List<ObservedAttribute> =
    asMap()
        .map { (key, value) ->
            ObservedAttribute(
                name = key.key,
                type =
                    when (key.type) {
                        AttributeType.STRING -> "string"
                        AttributeType.BOOLEAN -> "boolean"
                        AttributeType.LONG -> "int"
                        AttributeType.DOUBLE -> "double"
                        AttributeType.STRING_ARRAY -> "string_array"
                        AttributeType.BOOLEAN_ARRAY -> "boolean_array"
                        AttributeType.LONG_ARRAY -> "int_array"
                        AttributeType.DOUBLE_ARRAY -> "double_array"
                        AttributeType.VALUE -> "value"
                    },
                value = value,
            )
        }.sortedBy { it.name }

private fun Any.toJsonValue(): Any =
    when (this) {
        is Iterable<*> -> {
            JSONArray().apply {
                this@toJsonValue.forEach { put(it?.toJsonValue() ?: JSONObject.NULL) }
            }
        }

        is Number, is Boolean, is String -> {
            this
        }

        else -> {
            toString()
        }
    }

internal fun Description.telemetryFileName(): String {
    val testId = "$className-$methodName".replace(Regex("[^A-Za-z0-9_.-]"), "_")
    return "$testId-${System.nanoTime()}.telemetry.json"
}
