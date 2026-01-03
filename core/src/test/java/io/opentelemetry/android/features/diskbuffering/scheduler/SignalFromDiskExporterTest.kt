/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SignalFromDiskExporterTest {
    @MockK
    private lateinit var spanStorage: SignalStorage.Span

    @MockK
    private lateinit var spanExporter: SpanExporter

    @MockK
    private lateinit var metricStorage: SignalStorage.Metric

    @MockK
    private lateinit var metricExporter: MetricExporter

    @MockK
    private lateinit var logStorage: SignalStorage.LogRecord

    @MockK
    private lateinit var logExporter: LogRecordExporter

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `export spans successfully`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(makeIterator(size = 2))
        every { spanExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        assertThat(instance.exportSpansFromDisk()).isTrue()
        verify(exactly = 2) { spanExporter.export(any()) }
    }

    @Test
    fun `export metrics successfully`() {
        val instance = makeInstance()
        every { metricStorage.iterator() }.returns(makeIterator(size = 2))
        every { metricExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        assertThat(instance.exportMetricsFromDisk()).isTrue()
        verify(exactly = 2) { metricExporter.export(any()) }
    }

    @Test
    fun `export logs successfully`() {
        val instance = makeInstance()
        every { logStorage.iterator() }.returns(makeIterator(size = 2))
        every { logExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        assertThat(instance.exportLogsFromDisk()).isTrue()
        verify(exactly = 2) { logExporter.export(any()) }
    }

    @Test
    fun `stop exporting spans on first failure`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(makeIterator(size = 3))
        every { spanExporter.export(any()) }
            .returns(CompletableResultCode.ofSuccess())
            .andThen(CompletableResultCode.ofFailure())
        assertThat(instance.exportSpansFromDisk()).isFalse()
        verify(exactly = 2) { spanExporter.export(any()) }
    }

    @Test
    fun `stop exporting metrics on first failure`() {
        val instance = makeInstance()
        every { metricStorage.iterator() }.returns(makeIterator(size = 3))
        every { metricExporter.export(any()) }
            .returns(CompletableResultCode.ofSuccess())
            .andThen(CompletableResultCode.ofFailure())
        assertThat(instance.exportMetricsFromDisk()).isFalse()
        verify(exactly = 2) { metricExporter.export(any()) }
    }

    @Test
    fun `stop exporting logs on first failure`() {
        val instance = makeInstance()
        every { logStorage.iterator() }.returns(makeIterator(size = 3))
        every { logExporter.export(any()) }
            .returns(CompletableResultCode.ofSuccess())
            .andThen(CompletableResultCode.ofFailure())
        assertThat(instance.exportLogsFromDisk()).isFalse()
        verify(exactly = 2) { logExporter.export(any()) }
    }

    @Test
    fun `Return true when all exports succeed`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(makeIterator(size = 1))
        every { metricStorage.iterator() }.returns(makeIterator(size = 1))
        every { logStorage.iterator() }.returns(makeIterator(size = 1))

        every { spanExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        every { metricExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        every { logExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())

        assertThat(instance.exportAllSignalsFromDisk()).isTrue()
        verify { spanExporter.export(any()) }
        verify { metricExporter.export(any()) }
        verify { logExporter.export(any()) }
    }

    @Test
    fun `Return false when spans export fails`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(makeIterator(size = 1))

        every { spanExporter.export(any()) }.returns(CompletableResultCode.ofFailure())

        assertThat(instance.exportAllSignalsFromDisk()).isFalse()
        verify { spanExporter.export(any()) }
    }

    @Test
    fun `Return false when metrics export fails`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(makeIterator(size = 1))
        every { metricStorage.iterator() }.returns(makeIterator(size = 1))

        every { spanExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        every { metricExporter.export(any()) }.returns(CompletableResultCode.ofFailure())

        assertThat(instance.exportAllSignalsFromDisk()).isFalse()
        verify { spanExporter.export(any()) }
        verify { metricExporter.export(any()) }
    }

    @Test
    fun `Return false when logs export fails`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(makeIterator(size = 1))
        every { metricStorage.iterator() }.returns(makeIterator(size = 1))
        every { logStorage.iterator() }.returns(makeIterator(size = 1))

        every { spanExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        every { metricExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        every { logExporter.export(any()) }.returns(CompletableResultCode.ofFailure())

        assertThat(instance.exportAllSignalsFromDisk()).isFalse()
        verify { spanExporter.export(any()) }
        verify { metricExporter.export(any()) }
        verify { logExporter.export(any()) }
    }

    private inline fun <reified T : Any> makeIterator(size: Int): MutableIterator<List<T>> =
        MutableList(size) { listOf(mockk<T>()) }.iterator()

    private fun makeInstance(): SignalFromDiskExporter =
        SignalFromDiskExporter(
            spanStorage,
            spanExporter,
            logStorage,
            logExporter,
            metricStorage,
            metricExporter,
        )
}
