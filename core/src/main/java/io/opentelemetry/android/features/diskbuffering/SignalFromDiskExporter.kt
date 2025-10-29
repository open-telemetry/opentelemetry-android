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
import java.io.IOException
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
         * A batch contains all the signals that arrived in one call to [SpanFromDiskExporter.exportStoredBatch]. So if
         * that function is called 5 times, then there will be 5 batches in disk. This function reads
         * and exports ONE batch every time is called.
         *
         * @return TRUE if it found data in disk and the exporter succeeded. FALSE if any of those conditions were
         * not met.
         */
        @WorkerThread
        @Throws(IOException::class)
        fun exportBatchOfSpans(): Boolean = export(spanStorage, spanExporter::export)

        /**
         * A batch contains all the signals that arrived in one call to [MetricFromDiskExporter.exportStoredBatch]. So if
         * that function is called 5 times, then there will be 5 batches in disk. This function reads
         * and exports ONE batch every time is called.
         *
         * @return TRUE if it found data in disk and the exporter succeeded. FALSE if any of those conditions were
         * not met.
         */
        @WorkerThread
        @Throws(IOException::class)
        fun exportBatchOfMetrics(): Boolean = export(metricStorage, metricExporter::export)

        /**
         * A batch contains all the signals that arrived in one call to [LogRecordFromDiskExporter.exportStoredBatch]. So if
         * that function is called 5 times, then there will be 5 batches in disk. This function reads
         * and exports ONE batch every time is called.
         *
         * @return TRUE if it found data in disk and the exporter succeeded. FALSE if any of those conditions were
         * not met.
         */
        @WorkerThread
        @Throws(IOException::class)
        fun exportBatchOfLogs(): Boolean = export(logStorage, logExporter::export)

        private fun <T> export(
            storage: SignalStorage<T>,
            exporter: (Collection<T>) -> CompletableResultCode,
        ): Boolean {
            var rc = false
            val iter = storage.iterator()
            while (iter.hasNext()) {
                val result = exporter(iter.next()).join(exportTimeoutInMillis, TimeUnit.MILLISECONDS)
                // TODO: What to do if a random export in the middle here just fails?
                if (result.isDone && result.isSuccess) {
                    rc = true
                }
            }
            return rc
        }

        /**
         * Convenience method that attempts to export all kinds of signals from disk.
         *
         * @return TRUE if at least one of the signals were successfully exported, FALSE if no signal
         * of any kind was exported.
         */
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
