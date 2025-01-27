/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.opentelemetry.android.internal.services.periodicwork.PeriodicWork
import java.util.concurrent.atomic.AtomicBoolean

class DefaultExportScheduleHandler(
    private val exportScheduler: DefaultExportScheduler,
    private val periodicWorkProvider: () -> PeriodicWork,
) : ExportScheduleHandler {
    private val periodicWorkService by lazy { periodicWorkProvider() }
    private val enabled = AtomicBoolean(false)

    override fun enable() {
        if (!enabled.getAndSet(true)) {
            periodicWorkService.enqueue(exportScheduler)
        }
    }
}
