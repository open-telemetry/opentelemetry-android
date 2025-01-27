/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import org.junit.Test

class BufferDelegatingLogExporterTest {
    @Test
    fun `test setDelegate`() {
        val inMemoryBufferDelegatingLogExporter = BufferDelegatingLogExporter()
        val logRecordExporter = InMemoryLogRecordExporter.create()

        val logRecordData = mockk<LogRecordData>()
        inMemoryBufferDelegatingLogExporter.export(listOf(logRecordData))
        inMemoryBufferDelegatingLogExporter.setDelegate(logRecordExporter)

        assertThat(logRecordExporter.finishedLogRecordItems)
            .containsExactly(logRecordData)
    }

    @Test
    fun `test buffer limit handling`() {
        val inMemoryBufferDelegatingLogExporter = BufferDelegatingLogExporter(10)
        val logRecordExporter = InMemoryLogRecordExporter.create()

        repeat(11) {
            val logRecordData = mockk<LogRecordData>()
            inMemoryBufferDelegatingLogExporter.export(listOf(logRecordData))
        }

        inMemoryBufferDelegatingLogExporter.setDelegate(logRecordExporter)

        assertThat(logRecordExporter.finishedLogRecordItems)
            .hasSize(10)
    }

    @Test
    fun `test flush with delegate`() {
        val inMemoryBufferDelegatingLogExporter = BufferDelegatingLogExporter()
        val delegate = spyk<InMemoryLogRecordExporter>()

        val logRecordData = mockk<LogRecordData>()
        inMemoryBufferDelegatingLogExporter.export(listOf(logRecordData))

        inMemoryBufferDelegatingLogExporter.setDelegate(delegate)

        inMemoryBufferDelegatingLogExporter.flush()

        verify { delegate.flush() }
    }

    @Test
    fun `test export with delegate`() {
        val inMemoryBufferDelegatingLogExporter = BufferDelegatingLogExporter()
        val delegate = spyk<InMemoryLogRecordExporter>()

        val logRecordData = mockk<LogRecordData>()
        inMemoryBufferDelegatingLogExporter.export(listOf(logRecordData))

        verify(exactly = 0) { delegate.export(any()) }

        inMemoryBufferDelegatingLogExporter.setDelegate(delegate)

        verify(exactly = 1) { delegate.export(any()) }

        val logRecordData2 = mockk<LogRecordData>()
        inMemoryBufferDelegatingLogExporter.export(listOf(logRecordData2))

        verify(exactly = 2) { delegate.export(any()) }
    }

    @Test
    fun `test shutdown with delegate`() {
        val inMemoryBufferDelegatingLogExporter = BufferDelegatingLogExporter()
        val delegate = spyk<InMemoryLogRecordExporter>()

        inMemoryBufferDelegatingLogExporter.setDelegate(delegate)

        inMemoryBufferDelegatingLogExporter.shutdown()

        verify { delegate.shutdown() }
    }
}
