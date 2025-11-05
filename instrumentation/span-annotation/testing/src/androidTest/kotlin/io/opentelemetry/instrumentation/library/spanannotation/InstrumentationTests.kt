/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.spanannotation

import androidx.test.runner.AndroidJUnit4
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstrumentationTests {
    private lateinit var spanExporter: InMemorySpanExporter
    private lateinit var testApp: TestApp

    @Before
    fun setup() {
        spanExporter = InMemorySpanExporter.create()
        val tracerProvider =
            SdkTracerProvider
                .builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build()

        SpanAnnotationInstrumentation.tracer = tracerProvider.get("io.opentelemetry.android.instrumentation.span-annotation")
        testApp = TestApp()
    }

    @Test
    fun testWithSpanAnnotation() {
        val result = testApp.annotatedMethod("World")
        assertEquals("Hello World", result)

        val exportedSpans = spanExporter.finishedSpanItems
        assertEquals(1, exportedSpans.size)

        val span = exportedSpans[0]
        assertEquals("Span-Name", span.name)
        assertEquals("World", span.attributes.get(AttributeKey.stringKey("attribute")))
    }
}
