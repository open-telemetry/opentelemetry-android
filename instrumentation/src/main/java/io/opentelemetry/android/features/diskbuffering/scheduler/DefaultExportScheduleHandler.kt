/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService
import java.util.concurrent.atomic.AtomicBoolean

class DefaultExportScheduleHandler(private val exportScheduler: DefaultExportScheduler) :
    ExportScheduleHandler {
    private val enabled = AtomicBoolean(false)

    override fun enable() {
        if (!enabled.getAndSet(true)) {
            ServiceManager.get().getService(PeriodicWorkService::class.java)
                .enqueue(exportScheduler)
        }
    }
}
