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

class BufferDelegatingSpanExporterTest {
    @Test
    fun `test setDelegate`() {
        val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter()
        val spanExporter = InMemorySpanExporter.create()

        val spanData = mockk<SpanData>()
        bufferDelegatingSpanExporter.export(listOf(spanData))
        bufferDelegatingSpanExporter.setDelegate(spanExporter)

        assertThat(spanExporter.finishedSpanItems)
            .containsExactly(spanData)
    }

    @Test
    fun `test buffer limit handling`() {
        val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter(10)
        val spanExporter = InMemorySpanExporter.create()

        repeat(11) {
            val spanData = mockk<SpanData>()
            bufferDelegatingSpanExporter.export(listOf(spanData))
        }

        bufferDelegatingSpanExporter.setDelegate(spanExporter)

        assertThat(spanExporter.finishedSpanItems)
            .hasSize(10)
    }

    @Test
    fun `test flush with delegate`() {
        val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter()
        val delegate = spyk<InMemorySpanExporter>()

        val spanData = mockk<SpanData>()
        bufferDelegatingSpanExporter.export(listOf(spanData))

        bufferDelegatingSpanExporter.setDelegate(delegate)

        verify(exactly = 0) { delegate.flush() }

        bufferDelegatingSpanExporter.flush()

        verify { delegate.flush() }
    }

    @Test
    fun `test export with delegate`() {
        val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter()
        val delegate = spyk<InMemorySpanExporter>()

        val spanData = mockk<SpanData>()
        bufferDelegatingSpanExporter.export(listOf(spanData))

        verify(exactly = 0) { delegate.export(any()) }

        bufferDelegatingSpanExporter.setDelegate(delegate)

        verify(exactly = 1) { delegate.export(any()) }

        val spanData2 = mockk<SpanData>()
        bufferDelegatingSpanExporter.export(listOf(spanData2))

        verify(exactly = 2) { delegate.export(any()) }
    }

    @Test
    fun `test shutdown with delegate`() {
        val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter()
        val delegate = spyk<InMemorySpanExporter>()

        bufferDelegatingSpanExporter.setDelegate(delegate)

        bufferDelegatingSpanExporter.shutdown()

        verify { delegate.shutdown() }
    }

    @Test
    fun `test flush without delegate`() {
        val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter()

        val spanData = mockk<SpanData>()
        bufferDelegatingSpanExporter.export(listOf(spanData))

        val flushResult = bufferDelegatingSpanExporter.flush()

        assertThat(flushResult.isSuccess).isTrue()
    }

    @Test
    fun `test shutdown without delegate`() {
        val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter()

        val spanData = mockk<SpanData>()
        bufferDelegatingSpanExporter.export(listOf(spanData))

        val shutdownResult = bufferDelegatingSpanExporter.shutdown()

        assertThat(shutdownResult.isSuccess).isTrue()
    }
}
