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

class InMemoryBufferDelegatingLogExporterTest {
    @Test
    fun `test setDelegateAndFlush`() {
        val inMemoryBufferDelegatingLogExporter = InMemoryBufferDelegatingLogExporter()
        val logRecordExporter = InMemoryLogRecordExporter.create()

        val logRecordData: LogRecordData = mockk<LogRecordData>()
        inMemoryBufferDelegatingLogExporter.export(listOf(logRecordData))
        inMemoryBufferDelegatingLogExporter.setDelegateAndFlush(logRecordExporter)

        val logs: List<LogRecordData> = logRecordExporter.getFinishedLogRecordItems()
        assertThat(logs).hasSize(1)
        assertThat(logs[0]).isEqualTo(logRecordData)
    }

    @Test
    fun `test buffer limit handling`() {
        val inMemoryBufferDelegatingLogExporter = InMemoryBufferDelegatingLogExporter(10)
        val logRecordExporter = InMemoryLogRecordExporter.create()

        for (i in 1..11) {
            val logRecordData: LogRecordData = mockk<LogRecordData>()
            inMemoryBufferDelegatingLogExporter.export(listOf(logRecordData))
        }

        inMemoryBufferDelegatingLogExporter.setDelegateAndFlush(logRecordExporter)

        val logs = logRecordExporter.getFinishedLogRecordItems()
        assertThat(logs).hasSize(10)
    }

    @Test
    fun `test flush with delegate`() {
        val inMemoryBufferDelegatingLogExporter = InMemoryBufferDelegatingLogExporter()
        val delegate = spyk<InMemoryLogRecordExporter>()

        val logRecordData: LogRecordData = mockk<LogRecordData>()
        inMemoryBufferDelegatingLogExporter.export(listOf(logRecordData))

        inMemoryBufferDelegatingLogExporter.setDelegateAndFlush(delegate)

        inMemoryBufferDelegatingLogExporter.flush()

        verify { delegate.flush() }
    }

    @Test
    fun `test export with delegate`() {
        val inMemoryBufferDelegatingLogExporter = InMemoryBufferDelegatingLogExporter()
        val delegate = spyk<InMemoryLogRecordExporter>()

        val logRecordData: LogRecordData = mockk<LogRecordData>()
        inMemoryBufferDelegatingLogExporter.export(listOf(logRecordData))

        verify(exactly = 0) { delegate.export(any()) }

        inMemoryBufferDelegatingLogExporter.setDelegateAndFlush(delegate)

        verify(exactly = 1) { delegate.export(any()) }

        val logRecordData2: LogRecordData = mockk<LogRecordData>()
        inMemoryBufferDelegatingLogExporter.export(listOf(logRecordData2))

        verify(exactly = 2) { delegate.export(any()) }
    }

    @Test
    fun `test shutdown with delegate`() {
        val inMemoryBufferDelegatingLogExporter = InMemoryBufferDelegatingLogExporter()
        val delegate = spyk<InMemoryLogRecordExporter>()

        inMemoryBufferDelegatingLogExporter.setDelegateAndFlush(delegate)

        inMemoryBufferDelegatingLogExporter.shutdown()

        verify { delegate.shutdown() }
    }
}
