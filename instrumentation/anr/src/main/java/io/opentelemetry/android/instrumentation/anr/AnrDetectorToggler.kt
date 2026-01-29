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
    private var future: ScheduledFuture<*>? = null

    override fun onApplicationForegrounded() {
        if (future == null) {
            future = anrScheduler.scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS)
        }
    }

    override fun onApplicationBackgrounded() {
        future?.let {
            it.cancel(true)
            future = null
        }
    }
}
