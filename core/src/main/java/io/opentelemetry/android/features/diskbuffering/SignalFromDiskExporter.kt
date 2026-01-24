/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering

import androidx.annotation.WorkerThread
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.TimeUnit

/**
 * Entrypoint to read and export previously cached signals.
 */
class SignalFromDiskExporter
    @JvmOverloads
    internal constructor(
        private val spanStorage: SignalStorage.Span,
        private val spanExporter: SpanExporter,
        private val logStorage: SignalStorage.LogRecord,
        private val logExporter: LogRecordExporter,
        private val metricStorage: SignalStorage.Metric,
        private val metricExporter: MetricExporter,
        private val exportTimeoutInMillis: Long = TimeUnit.SECONDS.toMillis(5),
    ) {
        /**
         * Exports all persisted spans from the disk and deletes the successfully consumed batches
         * from the disk. It aborts the exporting on the first failure.
         *
         * @return `true`, if the exporting completely succeeded, `false` otherwise.
         */
        @WorkerThread
        fun exportSpansFromDisk(): Boolean = export(spanStorage, spanExporter::export)

        /**
         * Exports all persisted metrics from the disk and deletes the successfully consumed batches
         * from the disk. It aborts the exporting on the first failure.
         *
         * @return `true`, if the exporting completely succeeded, `false` otherwise.
         */
        @WorkerThread
        fun exportMetricsFromDisk(): Boolean = export(metricStorage, metricExporter::export)

        /**
         * Exports all persisted logs from the disk and deletes the successfully consumed batches
         * from the disk. It aborts the exporting on the first failure.
         *
         * @return `true`, if the exporting completely succeeded, `false` otherwise.
         */
        @WorkerThread
        fun exportLogsFromDisk(): Boolean = export(logStorage, logExporter::export)

        /**
         * Exports all persisted spans/metrics/logs from the disk and deletes the successfully
         * consumed batches from the disk. It aborts the exporting on the first failure.
         *
         * @return `true`, if the exporting completely succeeded, `false` otherwise.
         */
        private fun <T> export(
            storage: SignalStorage<T>,
            exporter: (Collection<T>) -> CompletableResultCode,
        ): Boolean {
            for (batch in storage) {
                val result = exporter(batch).join(exportTimeoutInMillis, TimeUnit.MILLISECONDS)
                if (!result.isSuccess) {
                    return false
                }
            }
            return true
        }

        /**
         * Convenience method that attempts to export all kinds of signals from disk.
         *
         * @return `true`, if all exporting completed successfully, `false` otherwise.`
         */
        @WorkerThread
        fun exportAllSignalsFromDisk(): Boolean {
            val successSpans = exportSpansFromDisk()
            val successMetrics = exportMetricsFromDisk()
            val successLogs = exportLogsFromDisk()
            return successSpans && successMetrics && successLogs
        }

        companion object {
            private var instance: SignalFromDiskExporter? = null

            @JvmStatic
            fun get(): SignalFromDiskExporter? = instance

            @JvmStatic
            fun set(signalFromDiskExporter: SignalFromDiskExporter) {
                check(instance == null) { "An instance is already set. You can only set it once." }
                instance = signalFromDiskExporter
            }

            @JvmStatic
            fun resetForTesting() {
                instance = null
            }
        }
    }
