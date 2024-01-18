/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import android.util.Log
import io.opentelemetry.android.RumConstants
import io.opentelemetry.android.features.diskbuffering.SignalDiskExporter
import io.opentelemetry.android.internal.services.periodicwork.PeriodicRunnable
import java.io.IOException
import java.util.concurrent.TimeUnit

class DefaultExportScheduler : PeriodicRunnable() {
    companion object {
        private val DELAY_BEFORE_NEXT_EXPORT_IN_MILLIS = TimeUnit.SECONDS.toMillis(10)
    }

    override fun onRun() {
        val exporter = SignalDiskExporter.get() ?: return

        try {
            do {
                val didExport = exporter.exportBatchOfEach()
            } while (didExport)
        } catch (e: IOException) {
            Log.e(RumConstants.OTEL_RUM_LOG_TAG, "Error while exporting signals from disk.", e)
        }
    }

    override fun shouldStopRunning(): Boolean {
        return SignalDiskExporter.get() == null
    }

    override fun minimumDelayUntilNextRunInMillis(): Long {
        return DELAY_BEFORE_NEXT_EXPORT_IN_MILLIS
    }
}
