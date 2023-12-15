/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodicwork

import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.android.internal.tools.time.SystemTime

/**
 * Utility create a Runnable that needs to run multiple times.
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

    protected abstract fun onRun()

    protected abstract fun shouldStopRunning(): Boolean

    protected abstract fun minimumDelayUntilNextRunInMillis(): Long
}
