/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.internal.services.periodic.PeriodicTaskScheduler
import java.util.concurrent.atomic.AtomicBoolean

class DiskBufferingEnablement(
    private val signalFromDiskExporter: SignalFromDiskExporter,
    private val taskScheduler: PeriodicTaskScheduler,
) : ScheduleEnablement {
    private val enabled = AtomicBoolean(false)
    @Volatile
    private var periodicExporter: PeriodicExporter? = null

    override fun enable() {
        if (!enabled.getAndSet(true)) {
            val exporter = PeriodicExporter(signalFromDiskExporter)
            periodicExporter = exporter
            taskScheduler.start(exporter)
        }
    }

    override fun disable() {
        if (enabled.getAndSet(false)) {
            periodicExporter?.stop()
            periodicExporter = null
        }
    }
}
