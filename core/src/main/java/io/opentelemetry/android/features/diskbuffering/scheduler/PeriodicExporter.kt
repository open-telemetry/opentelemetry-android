package io.opentelemetry.android.features.diskbuffering.scheduler

import android.util.Log
import io.opentelemetry.android.common.RumConstants.OTEL_RUM_LOG_TAG
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.internal.services.periodic.PeriodicRunnable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PeriodicExporter(
    private val delegate: SignalFromDiskExporter,
    private val exportPeriod: Duration = 10.seconds,
) : PeriodicRunnable {
    private val stop: AtomicBoolean = AtomicBoolean(false)

    override fun period(): Duration = exportPeriod

    override fun run() {
        if (!delegate.exportAllSignalsFromDisk()) {
            Log.e(OTEL_RUM_LOG_TAG, "Error while exporting signals from disk.")
        }
    }

    override fun stop() {
        stop.set(true)
    }

    override fun shouldStop(): Boolean = stop.get()
}
