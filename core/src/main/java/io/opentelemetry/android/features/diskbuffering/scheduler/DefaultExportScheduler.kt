/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import android.util.Log
import io.opentelemetry.android.common.RumConstants.OTEL_RUM_LOG_TAG
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.internal.services.periodicwork.PeriodicRunnable
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWork
import java.io.IOException
import java.util.concurrent.TimeUnit

class DefaultExportScheduler(
    periodicWorkProvider: () -> PeriodicWork,
) : PeriodicRunnable(periodicWorkProvider) {
    @Volatile
    private var isShutDown: Boolean = false

    companion object {
        private val DELAY_BEFORE_NEXT_EXPORT_IN_MILLIS = TimeUnit.SECONDS.toMillis(10)
    }

    override fun onRun() {
        val exporter = SignalFromDiskExporter.get() ?: return

        try {
            do {
                val didExport = exporter.exportBatchOfEach()
            } while (didExport)
        } catch (e: IOException) {
            Log.e(OTEL_RUM_LOG_TAG, "Error while exporting signals from disk.", e)
        }
    }

    fun shutdown() {
        isShutDown = true
    }

    override fun shouldStopRunning(): Boolean = isShutDown || (SignalFromDiskExporter.get() == null)

    override fun minimumDelayUntilNextRunInMillis(): Long = DELAY_BEFORE_NEXT_EXPORT_IN_MILLIS
}
