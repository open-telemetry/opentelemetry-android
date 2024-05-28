/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering

import androidx.annotation.WorkerThread
import io.opentelemetry.contrib.disk.buffering.LogRecordFromDiskExporter
import io.opentelemetry.contrib.disk.buffering.MetricFromDiskExporter
import io.opentelemetry.contrib.disk.buffering.SpanFromDiskExporter
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Entrypoint to read and export previously cached signals.
 */
class SignalFromDiskExporter
    @JvmOverloads
    internal constructor(
        private val spanFromDiskExporter: SpanFromDiskExporter?,
        private val metricFromDiskExporter: MetricFromDiskExporter?,
        private val logRecordFromDiskExporter: LogRecordFromDiskExporter?,
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
        fun exportBatchOfSpans(): Boolean {
            return spanFromDiskExporter?.exportStoredBatch(
                exportTimeoutInMillis,
                TimeUnit.MILLISECONDS,
            ) ?: false
        }

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
        fun exportBatchOfMetrics(): Boolean {
            return metricFromDiskExporter?.exportStoredBatch(
                exportTimeoutInMillis,
                TimeUnit.MILLISECONDS,
            ) ?: false
        }

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
        fun exportBatchOfLogs(): Boolean {
            return logRecordFromDiskExporter?.exportStoredBatch(
                exportTimeoutInMillis,
                TimeUnit.MILLISECONDS,
            ) ?: false
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
            fun get(): SignalFromDiskExporter? {
                return instance
            }

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
