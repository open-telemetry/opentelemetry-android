/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.opentelemetry.android.internal.services.periodicwork.PeriodicRunnable
import java.util.concurrent.TimeUnit

class DefaultExportScheduler : PeriodicRunnable() {
    companion object {
        private val DELAY_BEFORE_NEXT_EXPORT_IN_MILLIS = TimeUnit.SECONDS.toMillis(10)
    }

    override fun onRun() {
        // TODO for next PR.
    }

    override fun shouldStopRunning(): Boolean {
        return false
    }

    override fun minimumDelayUntilNextRunInMillis(): Long {
        return DELAY_BEFORE_NEXT_EXPORT_IN_MILLIS
    }
}
