/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.test.common

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.RumBuilder
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Intended for Android Instrumentation tests only.
 *
 * This rule initializes a [io.opentelemetry.android.OpenTelemetryRum] on every test and configures it to have
 * in-memory exporters.
 */
class OpenTelemetryRumRule : TestRule {
    lateinit var openTelemetryRum: OpenTelemetryRum
    lateinit var inMemorySpanExporter: InMemorySpanExporter
    lateinit var inMemoryLogExporter: InMemoryLogRecordExporter

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                setUpOpenTelemetry()
                base.evaluate()
            }
        }

    fun getSpan(): Span =
        openTelemetryRum
            .openTelemetry
            .getTracer("TestTracer")
            .spanBuilder("A Span")
            .startSpan()

    private fun setUpOpenTelemetry() {
        inMemorySpanExporter = InMemorySpanExporter.create()
        inMemoryLogExporter = InMemoryLogRecordExporter.create()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            openTelemetryRum =
                RumBuilder
                    .builder(ApplicationProvider.getApplicationContext())
                    .addLoggerProviderCustomizer { logger, _ ->
                        logger.addLogRecordProcessor(
                            SimpleLogRecordProcessor.create(inMemoryLogExporter),
                        )
                    }.addTracerProviderCustomizer { tracer, _ ->
                        tracer.addSpanProcessor(SimpleSpanProcessor.create(inMemorySpanExporter))
                    }.build()
        }
    }
}
