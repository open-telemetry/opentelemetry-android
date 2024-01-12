/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.opentelemetry.android.features.diskbuffering.SignalDiskExporter
import io.opentelemetry.contrib.disk.buffering.LogRecordDiskExporter
import io.opentelemetry.contrib.disk.buffering.MetricDiskExporter
import io.opentelemetry.contrib.disk.buffering.SpanDiskExporter
import io.opentelemetry.contrib.disk.buffering.StoredBatchExporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class SignalDiskExporterTest {
    companion object {
        private val DEFAULT_EXPORT_TIMEOUT_IN_MILLIS: Long = TimeUnit.SECONDS.toMillis(5)
    }

    @MockK
    private lateinit var spanDiskExporter: SpanDiskExporter

    @MockK
    private lateinit var metricDiskExporter: MetricDiskExporter

    @MockK
    private lateinit var logRecordDiskExporter: LogRecordDiskExporter

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `Verify exporting with custom timeout time`() {
        val timeoutInMillis = TimeUnit.SECONDS.toMillis(10)
        val instance =
            createInstance(
                spanDiskExporter,
                metricDiskExporter,
                logRecordDiskExporter,
                timeoutInMillis,
            )
        every { spanDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        assertThat(instance.exportBatchOfSpans()).isTrue()
        verifyExportStoredBatchCall(spanDiskExporter, timeoutInMillis)
    }

    @Test
    fun verifyExportingSpans() {
        val instance = createInstance(spanDiskExporter, metricDiskExporter, logRecordDiskExporter)
        every { spanDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        assertThat(instance.exportBatchOfSpans()).isTrue()
        verifyExportStoredBatchCall(spanDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun verifyExportingMetrics() {
        val instance = createInstance(spanDiskExporter, metricDiskExporter, logRecordDiskExporter)
        every { metricDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        assertThat(instance.exportBatchOfMetrics()).isTrue()
        verifyExportStoredBatchCall(metricDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun verifyExportingLogs() {
        val instance = createInstance(spanDiskExporter, metricDiskExporter, logRecordDiskExporter)
        every { logRecordDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        assertThat(instance.exportBatchOfLogs()).isTrue()
        verifyExportStoredBatchCall(logRecordDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun verifyExportingEach_whenAllReturnFalse() {
        val instance = createInstance(spanDiskExporter, metricDiskExporter, logRecordDiskExporter)
        every { spanDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { metricDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { logRecordDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        assertThat(instance.exportBatchOfEach()).isFalse()
        verifyExportStoredBatchCall(spanDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(metricDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(logRecordDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun verifyExportingEach_whenSpansReturnTrue() {
        val instance = createInstance(spanDiskExporter, metricDiskExporter, logRecordDiskExporter)
        every { spanDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        every { metricDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { logRecordDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        assertThat(instance.exportBatchOfEach()).isTrue()
        verifyExportStoredBatchCall(spanDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(metricDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(logRecordDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun verifyExportingEach_whenMetricsReturnTrue() {
        val instance = createInstance(spanDiskExporter, metricDiskExporter, logRecordDiskExporter)
        every { spanDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { metricDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        every { logRecordDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        assertThat(instance.exportBatchOfEach()).isTrue()
        verifyExportStoredBatchCall(spanDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(metricDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(logRecordDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun verifyExportingEach_whenLogsReturnTrue() {
        val instance = createInstance(spanDiskExporter, metricDiskExporter, logRecordDiskExporter)
        every { spanDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { metricDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { logRecordDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        assertThat(instance.exportBatchOfEach()).isTrue()
        verifyExportStoredBatchCall(spanDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(metricDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(logRecordDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun whenSpansExporterIsNull_returnFalse() {
        val instance = createInstance(null, metricDiskExporter, logRecordDiskExporter)
        assertThat(instance.exportBatchOfSpans()).isFalse()
    }

    @Test
    fun whenMetricsExporterIsNull_returnFalse() {
        val instance = createInstance(spanDiskExporter, null, logRecordDiskExporter)
        assertThat(instance.exportBatchOfMetrics()).isFalse()
    }

    @Test
    fun whenLogsExporterIsNull_returnFalse() {
        val instance = createInstance(spanDiskExporter, metricDiskExporter, null)
        assertThat(instance.exportBatchOfLogs()).isFalse()
    }

    private fun verifyExportStoredBatchCall(
        exporter: StoredBatchExporter,
        timeoutInMillis: Long,
    ) {
        verify {
            exporter.exportStoredBatch(timeoutInMillis, TimeUnit.MILLISECONDS)
        }
    }

    private fun createInstance(
        spanDiskExporter: SpanDiskExporter?,
        metricDiskExporter: MetricDiskExporter?,
        logRecordDiskExporter: LogRecordDiskExporter?,
        exportTimeoutInMillis: Long? = null,
    ): SignalDiskExporter {
        val builder = SignalDiskExporter.builder()
        if (spanDiskExporter != null) {
            builder.setSpanDiskExporter(spanDiskExporter)
        }
        if (metricDiskExporter != null) {
            builder.setMetricDiskExporter(metricDiskExporter)
        }
        if (logRecordDiskExporter != null) {
            builder.setLogRecordDiskExporter(logRecordDiskExporter)
        }
        if (exportTimeoutInMillis != null) {
            builder.setExportTimeoutInMillis(exportTimeoutInMillis)
        }

        return builder.build()
    }
}
