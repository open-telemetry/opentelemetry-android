/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.test.common

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter

class OpenTelemetryTestUtils {
    lateinit var openTelemetry: OpenTelemetry

    fun getSpan(): Span {
        return openTelemetry.getTracer("TestTracer").spanBuilder("A Span").startSpan()
    }

    fun setUpSpanExporter(spanExporter: SpanExporter): OpenTelemetry {
        openTelemetry =
            OpenTelemetrySdk.builder()
                .setTracerProvider(getSimpleTracerProvider(spanExporter))
                .build()

        // TODO: Remove the bottom two lines after making okhttp3 androidTests parallel too.
        GlobalOpenTelemetry.resetForTest()
        GlobalOpenTelemetry.set(openTelemetry)

        return openTelemetry
    }

    private fun getSimpleTracerProvider(spanExporter: SpanExporter): SdkTracerProvider {
        return SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build()
    }
}
