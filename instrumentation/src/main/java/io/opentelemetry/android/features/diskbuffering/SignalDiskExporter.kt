/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering

import androidx.annotation.WorkerThread
import io.opentelemetry.contrib.disk.buffering.LogRecordDiskExporter
import io.opentelemetry.contrib.disk.buffering.MetricDiskExporter
import io.opentelemetry.contrib.disk.buffering.SpanDiskExporter
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Entrypoint to read and export previously cached signals.
 */
class SignalDiskExporter internal constructor(
    private val spanDiskExporter: SpanDiskExporter?,
    private val metricDiskExporter: MetricDiskExporter?,
    private val logRecordDiskExporter: LogRecordDiskExporter?,
    private val exportTimeoutInMillis: Long,
) {
    @WorkerThread
    @Throws(IOException::class)
    fun exportBatchOfSpans(): Boolean {
        return spanDiskExporter?.exportStoredBatch(
            exportTimeoutInMillis,
            TimeUnit.MILLISECONDS,
        ) ?: false
    }

    @WorkerThread
    @Throws(IOException::class)
    fun exportBatchOfMetrics(): Boolean {
        return metricDiskExporter?.exportStoredBatch(
            exportTimeoutInMillis,
            TimeUnit.MILLISECONDS,
        ) ?: false
    }

    @WorkerThread
    @Throws(IOException::class)
    fun exportBatchOfLogs(): Boolean {
        return logRecordDiskExporter?.exportStoredBatch(
            exportTimeoutInMillis,
            TimeUnit.MILLISECONDS,
        ) ?: false
    }

    @WorkerThread
    @Throws(IOException::class)
    fun exportBatchOfEach(): Boolean {
        var atLeastOneWorked = exportBatchOfSpans()
        if (exportBatchOfMetrics()) {
            atLeastOneWorked = true
        }
        if (exportBatchOfLogs()) {
            atLeastOneWorked = true
        }
        return atLeastOneWorked
    }

    class Builder {
        private var spanDiskExporter: SpanDiskExporter? = null
        private var metricDiskExporter: MetricDiskExporter? = null
        private var logRecordDiskExporter: LogRecordDiskExporter? = null
        private var exportTimeoutInMillis = TimeUnit.SECONDS.toMillis(5)

        fun setSpanDiskExporter(spanDiskExporter: SpanDiskExporter) =
            apply {
                this.spanDiskExporter = spanDiskExporter
            }

        fun setMetricDiskExporter(metricDiskExporter: MetricDiskExporter) =
            apply {
                this.metricDiskExporter = metricDiskExporter
            }

        fun setLogRecordDiskExporter(logRecordDiskExporter: LogRecordDiskExporter) =
            apply {
                this.logRecordDiskExporter = logRecordDiskExporter
            }

        fun setExportTimeoutInMillis(exportTimeoutInMillis: Long) =
            apply {
                this.exportTimeoutInMillis = exportTimeoutInMillis
            }

        fun build(): SignalDiskExporter {
            return SignalDiskExporter(
                spanDiskExporter,
                metricDiskExporter,
                logRecordDiskExporter,
                exportTimeoutInMillis,
            )
        }
    }

    companion object {
        private var instance: SignalDiskExporter? = null

        @JvmStatic
        fun get(): SignalDiskExporter? {
            return instance
        }

        @JvmStatic
        fun set(signalDiskExporter: SignalDiskExporter) {
            check(instance == null) { "An instance is already set. You can only set it once." }
            instance = signalDiskExporter
        }

        @JvmStatic
        fun resetForTesting() {
            instance = null
        }

        @Throws(IOException::class)
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
