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
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.trace.data.SpanData
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
    fun `Verify exporting spans`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(mutableListOf(listOf(mockk<SpanData>())).iterator())
        every { spanExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        assertThat(instance.exportBatchOfSpans()).isTrue()
        verify { spanExporter.export(any()) }
    }

    @Test
    fun `Verify exporting metrics`() {
        val instance = makeInstance()
        every { metricStorage.iterator() }.returns(mutableListOf(listOf(mockk<MetricData>())).iterator())
        every { metricExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        assertThat(instance.exportBatchOfMetrics()).isTrue()
        verify { metricExporter.export(any()) }
    }

    @Test
    fun `Verify exporting logs`() {
        val instance = makeInstance()
        every { logStorage.iterator() }.returns(mutableListOf(listOf(mockk<LogRecordData>())).iterator())
        every { logExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        assertThat(instance.exportBatchOfLogs()).isTrue()
        verify { logExporter.export(any()) }
    }

    @Test
    fun `Return false when all exports fail`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(mutableListOf(listOf(mockk<SpanData>())).iterator())
        every { metricStorage.iterator() }.returns(mutableListOf(listOf(mockk<MetricData>())).iterator())
        every { logStorage.iterator() }.returns(mutableListOf(listOf(mockk<LogRecordData>())).iterator())

        every { spanExporter.export(any()) }.returns(CompletableResultCode.ofFailure())
        every { metricExporter.export(any()) }.returns(CompletableResultCode.ofFailure())
        every { logExporter.export(any()) }.returns(CompletableResultCode.ofFailure())

        assertThat(instance.exportBatchOfEach()).isFalse()
        verify { spanExporter.export(any()) }
        verify { metricExporter.export(any()) }
        verify { logExporter.export(any()) }
    }

    @Test
    fun `Return true when spans export succeeds`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(mutableListOf(listOf(mockk<SpanData>())).iterator())
        every { metricStorage.iterator() }.returns(mutableListOf(listOf(mockk<MetricData>())).iterator())
        every { logStorage.iterator() }.returns(mutableListOf(listOf(mockk<LogRecordData>())).iterator())

        every { spanExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        every { metricExporter.export(any()) }.returns(CompletableResultCode.ofFailure())
        every { logExporter.export(any()) }.returns(CompletableResultCode.ofFailure())

        assertThat(instance.exportBatchOfEach()).isTrue()
        verify { spanExporter.export(any()) }
        verify { metricExporter.export(any()) }
        verify { logExporter.export(any()) }
    }

    @Test
    fun `Return true when metrics export succeeds`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(mutableListOf(listOf(mockk<SpanData>())).iterator())
        every { metricStorage.iterator() }.returns(mutableListOf(listOf(mockk<MetricData>())).iterator())
        every { logStorage.iterator() }.returns(mutableListOf(listOf(mockk<LogRecordData>())).iterator())

        every { spanExporter.export(any()) }.returns(CompletableResultCode.ofFailure())
        every { metricExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())
        every { logExporter.export(any()) }.returns(CompletableResultCode.ofFailure())

        assertThat(instance.exportBatchOfEach()).isTrue()
        verify { spanExporter.export(any()) }
        verify { metricExporter.export(any()) }
        verify { logExporter.export(any()) }
    }

    private fun makeInstance(): SignalFromDiskExporter =
        SignalFromDiskExporter(
            spanStorage,
            spanExporter,
            logStorage,
            logExporter,
            metricStorage,
            metricExporter,
        )

    @Test
    fun `Return true when logs export succeeds`() {
        val instance = makeInstance()
        every { spanStorage.iterator() }.returns(mutableListOf(listOf(mockk<SpanData>())).iterator())
        every { metricStorage.iterator() }.returns(mutableListOf(listOf(mockk<MetricData>())).iterator())
        every { logStorage.iterator() }.returns(mutableListOf(listOf(mockk<LogRecordData>())).iterator())

        every { spanExporter.export(any()) }.returns(CompletableResultCode.ofFailure())
        every { metricExporter.export(any()) }.returns(CompletableResultCode.ofFailure())
        every { logExporter.export(any()) }.returns(CompletableResultCode.ofSuccess())

        assertThat(instance.exportBatchOfEach()).isTrue()
        verify { spanExporter.export(any()) }
        verify { metricExporter.export(any()) }
        verify { logExporter.export(any()) }
    }
}
