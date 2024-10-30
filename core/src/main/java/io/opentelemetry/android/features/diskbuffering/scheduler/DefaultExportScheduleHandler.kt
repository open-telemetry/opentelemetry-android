/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService
import java.util.concurrent.atomic.AtomicBoolean

class DefaultExportScheduleHandler(
    private val exportScheduler: DefaultExportScheduler,
    private val periodicWorkServiceProvider: () -> PeriodicWorkService,
) :
    ExportScheduleHandler {
    private val periodicWorkService by lazy { periodicWorkServiceProvider() }
    private val enabled = AtomicBoolean(false)

    override fun enable() {
        if (!enabled.getAndSet(true)) {
            periodicWorkService.enqueue(exportScheduler)
        }
    }

    companion object {
        @JvmStatic
        fun create(): DefaultExportScheduleHandler {
            val serviceManager = ServiceManager.get()
            return DefaultExportScheduleHandler(
                DefaultExportScheduler.create(serviceManager),
            ) {
                serviceManager.getPeriodicWorkService()
            }
        }
    }
}
