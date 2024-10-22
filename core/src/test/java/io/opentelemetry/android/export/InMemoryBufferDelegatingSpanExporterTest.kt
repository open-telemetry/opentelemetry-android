/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import org.junit.Test

class InMemoryBufferDelegatingSpanExporterTest {
    @Test
    fun `test setDelegateAndFlush`() {
        val inMemoryBufferDelegatingSpanExporter = InMemoryBufferDelegatingSpanExporter()
        val spanExporter = InMemorySpanExporter.create()

        val spanData: SpanData = mockk<SpanData>()
        inMemoryBufferDelegatingSpanExporter.export(listOf(spanData))
        inMemoryBufferDelegatingSpanExporter.setDelegateAndFlush(spanExporter)

        val spans: List<SpanData> = spanExporter.getFinishedSpanItems()
        assertThat(spans).hasSize(1)
        assertThat(spans[0]).isEqualTo(spanData)
    }

    @Test
    fun `test buffer limit handling`() {
        val inMemoryBufferDelegatingSpanExporter = InMemoryBufferDelegatingSpanExporter(10)
        val spanExporter = InMemorySpanExporter.create()

        for (i in 1..11) {
            val spanData: SpanData = mockk<SpanData>()
            inMemoryBufferDelegatingSpanExporter.export(listOf(spanData))
        }

        inMemoryBufferDelegatingSpanExporter.setDelegateAndFlush(spanExporter)

        val spans = spanExporter.getFinishedSpanItems()
        assertThat(spans).hasSize(10)
    }

    @Test
    fun `test flush with delegate`() {
        val inMemoryBufferDelegatingSpanExporter = InMemoryBufferDelegatingSpanExporter()
        val delegate = spyk<InMemorySpanExporter>()

        val spanData: SpanData = mockk<SpanData>()
        inMemoryBufferDelegatingSpanExporter.export(listOf(spanData))

        inMemoryBufferDelegatingSpanExporter.setDelegateAndFlush(delegate)

        inMemoryBufferDelegatingSpanExporter.flush()

        verify { delegate.flush() }
    }

    @Test
    fun `test export with delegate`() {
        val inMemoryBufferDelegatingSpanExporter = InMemoryBufferDelegatingSpanExporter()
        val delegate = spyk<InMemorySpanExporter>()

        val spanData: SpanData = mockk<SpanData>()
        inMemoryBufferDelegatingSpanExporter.export(listOf(spanData))

        verify(exactly = 0) { delegate.export(any()) }

        inMemoryBufferDelegatingSpanExporter.setDelegateAndFlush(delegate)

        verify(exactly = 1) { delegate.export(any()) }

        val spanData2: SpanData = mockk<SpanData>()
        inMemoryBufferDelegatingSpanExporter.export(listOf(spanData2))

        verify(exactly = 2) { delegate.export(any()) }
    }

    @Test
    fun `test shutdown with delegate`() {
        val inMemoryBufferDelegatingSpanExporter = InMemoryBufferDelegatingSpanExporter()
        val delegate = spyk<InMemorySpanExporter>()

        inMemoryBufferDelegatingSpanExporter.setDelegateAndFlush(delegate)

        inMemoryBufferDelegatingSpanExporter.shutdown()

        verify { delegate.shutdown() }
    }

    @Test
    fun `test flush without delegate`() {
        val inMemoryBufferDelegatingSpanExporter = InMemoryBufferDelegatingSpanExporter()

        val spanData: SpanData = mockk<SpanData>()
        inMemoryBufferDelegatingSpanExporter.export(listOf(spanData))

        val flushResult = inMemoryBufferDelegatingSpanExporter.flush()

        assertThat(flushResult.isSuccess).isTrue()
    }

    @Test
    fun `test shutdown without delegate`() {
        val inMemoryBufferDelegatingSpanExporter = InMemoryBufferDelegatingSpanExporter()

        val spanData: SpanData = mockk<SpanData>()
        inMemoryBufferDelegatingSpanExporter.export(listOf(spanData))

        val shutdownResult = inMemoryBufferDelegatingSpanExporter.shutdown()

        assertThat(shutdownResult.isSuccess).isTrue()
    }
}
