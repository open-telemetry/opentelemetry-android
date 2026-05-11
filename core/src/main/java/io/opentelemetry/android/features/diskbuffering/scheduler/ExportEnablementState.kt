/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.internal.services.schedule.WorkScheduler
import java.util.concurrent.atomic.AtomicBoolean

class ExportEnablementState(
    private val signalFromDiskExporter: SignalFromDiskExporter,
    private val workScheduler: WorkScheduler,
) : ExportScheduleHandler {
    private val enabled = AtomicBoolean(false)
    private val periodicExporter = PeriodicExporter(signalFromDiskExporter)

    override fun enable() {
        if (!enabled.getAndSet(true)) {
            workScheduler.start(periodicExporter)
        }
    }

    override fun disable() {
        super.disable()
        periodicExporter.stop()
    }
}
