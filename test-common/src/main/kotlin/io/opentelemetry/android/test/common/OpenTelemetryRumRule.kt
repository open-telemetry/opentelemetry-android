/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.test.common

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
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
    private val testHarness = OpenTelemetryRumTestHarness()

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
                evaluateWithTelemetryCapture(base, description)
            }
        }

    fun getSpan(): Span =
        openTelemetryRum
            .openTelemetry
            .getTracer("TestTracer")
            .spanBuilder("A Span")
            .startSpan()

    private fun setUpOpenTelemetry() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            testHarness.setUp(ApplicationProvider.getApplicationContext())
            openTelemetryRum = testHarness.openTelemetryRum
            inMemorySpanExporter = testHarness.inMemorySpanExporter
            inMemoryLogExporter = testHarness.inMemoryLogExporter
        }
    }

    private fun evaluateWithTelemetryCapture(
        base: Statement,
        description: Description,
    ) {
        try {
            base.evaluate()
        } catch (failure: Throwable) {
            try {
                captureTelemetry(description)
            } catch (captureFailure: Throwable) {
                failure.addSuppressed(captureFailure)
            }
            throw failure
        }
        captureTelemetry(description)
    }

    private fun captureTelemetry(description: Description) {
        if (isTelemetryCaptureEnabled()) {
            val fileName = description.telemetryFileName()
            PlatformTestStorageRegistry
                .getInstance()
                .openOutputFile("telemetry-docs/$fileName")
                .bufferedWriter()
                .use { it.write(testHarness.captureJson()) }
        }
    }

    private fun isTelemetryCaptureEnabled(): Boolean =
        InstrumentationRegistry
            .getArguments()
            .getString(COLLECT_TELEMETRY_DOCS_PROPERTY)
            ?.toBoolean() == true
}
