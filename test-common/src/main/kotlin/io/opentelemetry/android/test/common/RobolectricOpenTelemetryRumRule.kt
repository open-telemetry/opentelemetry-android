/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.test.common

import androidx.test.core.app.ApplicationProvider
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * Initializes an [OpenTelemetryRum] with in-memory exporters for Robolectric tests.
 */
class RobolectricOpenTelemetryRumRule
    @JvmOverloads
    constructor(
        private val collectTelemetryDocs: Boolean =
            System.getProperty(COLLECT_TELEMETRY_DOCS_PROPERTY)?.toBoolean() == true,
        private val outputDirectory: File? =
            System.getProperty(TELEMETRY_DOCS_OUTPUT_DIRECTORY_PROPERTY)?.let(::File),
    ) : TestRule {
        private val testHarness = OpenTelemetryRumTestHarness()

        val openTelemetryRum: OpenTelemetryRum
            get() = testHarness.openTelemetryRum

        val inMemorySpanExporter: InMemorySpanExporter
            get() = testHarness.inMemorySpanExporter

        val inMemoryLogExporter: InMemoryLogRecordExporter
            get() = testHarness.inMemoryLogExporter

        override fun apply(
            base: Statement,
            description: Description,
        ): Statement =
            object : Statement() {
                override fun evaluate() {
                    ShadowLooper.getShadowMainLooper().runPaused {
                        testHarness.setUp(ApplicationProvider.getApplicationContext())
                    }
                    evaluateWithTelemetryCapture(base, description)
                }
            }

        fun getSpan(): Span =
            openTelemetryRum
                .openTelemetry
                .getTracer("TestTracer")
                .spanBuilder("A Span")
                .startSpan()

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
            if (collectTelemetryDocs) {
                val directory =
                    checkNotNull(outputDirectory) {
                        "Set $TELEMETRY_DOCS_OUTPUT_DIRECTORY_PROPERTY when telemetry capture is enabled."
                    }
                directory.mkdirs()
                File(directory, description.telemetryFileName()).writeText(testHarness.captureJson())
            }
        }
    }
