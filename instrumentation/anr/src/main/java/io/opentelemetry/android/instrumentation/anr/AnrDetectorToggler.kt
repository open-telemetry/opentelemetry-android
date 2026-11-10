/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class AnrDetectorToggler(
    private val anrWatcher: Runnable,
    private val anrScheduler: ScheduledExecutorService,
) : ApplicationStateListener {
    private val lock = Any()

    @Volatile
    private var future: ScheduledFuture<*>? = null

    @Volatile
    private var stopped = false

    override fun onApplicationForegrounded() {
        synchronized(lock) {
            if (stopped) {
                return
            }
            if (future == null) {
                future = anrScheduler.scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS)
            }
        }
    }

    override fun onApplicationBackgrounded() {
        synchronized(lock) {
            future?.let {
                it.cancel(true)
                future = null
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            stopped = true
            onApplicationBackgrounded()
        }
    }
}
