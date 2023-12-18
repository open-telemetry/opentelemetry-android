/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodicwork

import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.android.internal.tools.time.SystemTime

/**
 * Utility for creating a Runnable that needs to run multiple times.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
abstract class PeriodicRunnable : Runnable {
    private var lastTimeItRan: Long? = null

    final override fun run() {
        if (isReadyToRun()) {
            onRun()
            lastTimeItRan = getCurrentTimeMillis()
        }
        if (!shouldStopRunning()) {
            enqueueForNextLoop()
        }
    }

    private fun isReadyToRun(): Boolean {
        return lastTimeItRan?.let {
            getCurrentTimeMillis() >= (it + minimumDelayUntilNextRunInMillis())
        } ?: true
    }

    private fun enqueueForNextLoop() {
        ServiceManager.get().getService(PeriodicWorkService::class.java).enqueue(this)
    }

    private fun getCurrentTimeMillis() = SystemTime.get().getCurrentTimeMillis()

    /**
     * Called only if a) The runnable has never run before, OR b) The minimum amount of time delay has passed after the last run.
     */
    abstract fun onRun()

    /**
     * Should return FALSE when further runs are needed, TRUE if no need for this task to ever run again.
     */
    abstract fun shouldStopRunning(): Boolean

    /**
     * The minimum amount of time to wait between runs, it might take longer than what's defined here
     * to run this task again depending on when the next batch of background work will get submitted.
     */
    abstract fun minimumDelayUntilNextRunInMillis(): Long
}
