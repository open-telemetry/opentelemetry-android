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
import java.nio.BufferOverflowException

class BufferDelegatingSpanExporterTest {
    private val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter()
    private val delegate = spyk<InMemorySpanExporter>()
    private val spanData = mockk<SpanData>()

    @Test
    fun `test no data`() {
        bufferDelegatingSpanExporter.setDelegate(delegate)

        verify(exactly = 0) { delegate.export(any()) }
        verify(exactly = 0) { delegate.flush() }
        verify(exactly = 0) { delegate.shutdown() }
    }

    @Test
    fun `test setDelegate`() {
        bufferDelegatingSpanExporter.export(listOf(spanData))
        bufferDelegatingSpanExporter.setDelegate(delegate)

        assertThat(delegate.finishedSpanItems)
            .containsExactly(spanData)
        verify(exactly = 0) { delegate.flush() }
        verify(exactly = 0) { delegate.shutdown() }
    }

    @Test
    fun `the export result should complete when the delegate is set`() {
        val result = bufferDelegatingSpanExporter.export(listOf(spanData))
        assertThat(result.isDone).isFalse()
        bufferDelegatingSpanExporter.setDelegate(delegate)
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `test buffer limit handling`() {
        val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter(10)
        val spanExporter = InMemorySpanExporter.create()
        val initialResult = bufferDelegatingSpanExporter.export(List(10) { mockk<SpanData>() })
        assertThat(initialResult.isDone).isFalse()

        val overflowResult = bufferDelegatingSpanExporter.export(listOf(mockk<SpanData>()))
        assertThat(overflowResult.isDone).isTrue()
        assertThat(overflowResult.isSuccess).isFalse()
        assertThat(overflowResult.failureThrowable).isInstanceOf(BufferOverflowException::class.java)

        bufferDelegatingSpanExporter.setDelegate(spanExporter)

        assertThat(spanExporter.finishedSpanItems)
            .hasSize(10)
    }

    @Test
    fun `test flush with delegate`() {
        bufferDelegatingSpanExporter.setDelegate(delegate)
        verify(exactly = 0) { delegate.flush() }
        val result = bufferDelegatingSpanExporter.flush()
        verify(exactly = 1) { delegate.flush() }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `test flush without delegate`() {
        val result = bufferDelegatingSpanExporter.flush()
        assertThat(result.isDone).isFalse()

        bufferDelegatingSpanExporter.setDelegate(delegate)
        verify(exactly = 1) { delegate.flush() }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `test export with delegate`() {
        bufferDelegatingSpanExporter.export(listOf(spanData))
        bufferDelegatingSpanExporter.setDelegate(delegate)

        assertThat(delegate.finishedSpanItems).containsExactly(spanData)

        val spanData2 = mockk<SpanData>()
        val result = bufferDelegatingSpanExporter.export(listOf(spanData2))

        assertThat(delegate.finishedSpanItems).containsExactly(spanData, spanData2)
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `test shutdown with delegate`() {
        bufferDelegatingSpanExporter.setDelegate(delegate)
        val result = bufferDelegatingSpanExporter.shutdown()
        verify(exactly = 1) { delegate.shutdown() }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `test shutdown without delegate`() {
        val result = bufferDelegatingSpanExporter.shutdown()
        assertThat(result.isDone).isFalse()

        bufferDelegatingSpanExporter.setDelegate(delegate)
        assertThat(result.isSuccess).isTrue()
    }
}
