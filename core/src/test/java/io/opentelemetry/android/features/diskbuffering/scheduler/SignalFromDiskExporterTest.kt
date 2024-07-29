/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.contrib.disk.buffering.LogRecordFromDiskExporter
import io.opentelemetry.contrib.disk.buffering.MetricFromDiskExporter
import io.opentelemetry.contrib.disk.buffering.SpanFromDiskExporter
import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class SignalFromDiskExporterTest {
    companion object {
        private val DEFAULT_EXPORT_TIMEOUT_IN_MILLIS: Long = TimeUnit.SECONDS.toMillis(5)
    }

    @MockK
    private lateinit var spanFromDiskExporter: SpanFromDiskExporter

    @MockK
    private lateinit var metricFromDiskExporter: MetricFromDiskExporter

    @MockK
    private lateinit var logRecordFromDiskExporter: LogRecordFromDiskExporter

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `Exporting with custom timeout time`() {
        val timeoutInMillis = TimeUnit.SECONDS.toMillis(10)
        val instance =
            createInstance(
                spanFromDiskExporter,
                metricFromDiskExporter,
                logRecordFromDiskExporter,
                timeoutInMillis,
            )
        every { spanFromDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        assertThat(instance.exportBatchOfSpans()).isTrue()
        verifyExportStoredBatchCall(spanFromDiskExporter, timeoutInMillis)
    }

    @Test
    fun `Verify exporting spans`() {
        val instance =
            createInstance(spanFromDiskExporter, metricFromDiskExporter, logRecordFromDiskExporter)
        every { spanFromDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        assertThat(instance.exportBatchOfSpans()).isTrue()
        verifyExportStoredBatchCall(spanFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun `Verify exporting metrics`() {
        val instance =
            createInstance(spanFromDiskExporter, metricFromDiskExporter, logRecordFromDiskExporter)
        every { metricFromDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        assertThat(instance.exportBatchOfMetrics()).isTrue()
        verifyExportStoredBatchCall(metricFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun `Verify exporting logs`() {
        val instance =
            createInstance(spanFromDiskExporter, metricFromDiskExporter, logRecordFromDiskExporter)
        every { logRecordFromDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        assertThat(instance.exportBatchOfLogs()).isTrue()
        verifyExportStoredBatchCall(logRecordFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun `Return false when all exports fail`() {
        val instance =
            createInstance(spanFromDiskExporter, metricFromDiskExporter, logRecordFromDiskExporter)
        every { spanFromDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { metricFromDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { logRecordFromDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        assertThat(instance.exportBatchOfEach()).isFalse()
        verifyExportStoredBatchCall(spanFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(metricFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(logRecordFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun `Return true when spans export succeeds`() {
        val instance =
            createInstance(spanFromDiskExporter, metricFromDiskExporter, logRecordFromDiskExporter)
        every { spanFromDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        every { metricFromDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { logRecordFromDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        assertThat(instance.exportBatchOfEach()).isTrue()
        verifyExportStoredBatchCall(spanFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(metricFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(logRecordFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun `Return true when metrics export succeeds`() {
        val instance =
            createInstance(spanFromDiskExporter, metricFromDiskExporter, logRecordFromDiskExporter)
        every { spanFromDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { metricFromDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        every { logRecordFromDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        assertThat(instance.exportBatchOfEach()).isTrue()
        verifyExportStoredBatchCall(spanFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(metricFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(logRecordFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun `Return true when logs export succeeds`() {
        val instance =
            createInstance(spanFromDiskExporter, metricFromDiskExporter, logRecordFromDiskExporter)
        every { spanFromDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { metricFromDiskExporter.exportStoredBatch(any(), any()) }.returns(false)
        every { logRecordFromDiskExporter.exportStoredBatch(any(), any()) }.returns(true)
        assertThat(instance.exportBatchOfEach()).isTrue()
        verifyExportStoredBatchCall(spanFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(metricFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
        verifyExportStoredBatchCall(logRecordFromDiskExporter, DEFAULT_EXPORT_TIMEOUT_IN_MILLIS)
    }

    @Test
    fun `Return false when spans exporter is not available`() {
        val instance = createInstance(null, metricFromDiskExporter, logRecordFromDiskExporter)
        assertThat(instance.exportBatchOfSpans()).isFalse()
    }

    @Test
    fun `Return false when metrics exporter is not available`() {
        val instance = createInstance(spanFromDiskExporter, null, logRecordFromDiskExporter)
        assertThat(instance.exportBatchOfMetrics()).isFalse()
    }

    @Test
    fun `Return false when logs exporter is not available`() {
        val instance = createInstance(spanFromDiskExporter, metricFromDiskExporter, null)
        assertThat(instance.exportBatchOfLogs()).isFalse()
    }

    private fun verifyExportStoredBatchCall(
        exporter: FromDiskExporter,
        timeoutInMillis: Long,
    ) {
        verify {
            exporter.exportStoredBatch(timeoutInMillis, TimeUnit.MILLISECONDS)
        }
    }

    private fun createInstance(
        spanFromDiskExporter: SpanFromDiskExporter?,
        metricFromDiskExporter: MetricFromDiskExporter?,
        logRecordFromDiskExporter: LogRecordFromDiskExporter?,
        exportTimeoutInMillis: Long? = null,
    ): SignalFromDiskExporter {
        return if (exportTimeoutInMillis == null) {
            SignalFromDiskExporter(
                spanFromDiskExporter,
                metricFromDiskExporter,
                logRecordFromDiskExporter,
            )
        } else {
            SignalFromDiskExporter(
                spanFromDiskExporter,
                metricFromDiskExporter,
                logRecordFromDiskExporter,
                exportTimeoutInMillis,
            )
        }
    }
}
