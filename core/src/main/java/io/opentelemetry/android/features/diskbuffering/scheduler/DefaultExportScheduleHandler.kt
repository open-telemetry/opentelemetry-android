/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.opentelemetry.android.internal.services.periodicwork.PeriodicWork

class DefaultExportScheduleHandler(
    private val periodicWorkProvider: () -> PeriodicWork,
    private val exportIntervalInSeconds: Long,
) : ExportScheduleHandler {
    private var scheduler: DefaultExportScheduler? = null

    override fun enable() {
        if (scheduler == null) {
            scheduler = DefaultExportScheduler(periodicWorkProvider, exportIntervalInSeconds)
            periodicWorkProvider().enqueue(scheduler!!)
        }
    }

    override fun disable() {
        scheduler = null
    }
}
