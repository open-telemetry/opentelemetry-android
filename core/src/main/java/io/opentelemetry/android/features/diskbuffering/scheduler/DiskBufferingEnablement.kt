/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.internal.services.periodic.PeriodicTaskScheduler
import java.util.concurrent.atomic.AtomicReference

class DiskBufferingEnablement(
    private val signalFromDiskExporter: SignalFromDiskExporter,
    private val taskScheduler: PeriodicTaskScheduler,
) : ScheduleEnablement {
    private val periodicExporter = AtomicReference<PeriodicExporter?>(null)

    override fun enable() {
        val exporter = PeriodicExporter(signalFromDiskExporter)
        if (periodicExporter.compareAndSet(null, exporter)) {
            taskScheduler.start(exporter)
        }
    }

    override fun disable() {
        periodicExporter.getAndSet(null)?.stop()
    }
}
