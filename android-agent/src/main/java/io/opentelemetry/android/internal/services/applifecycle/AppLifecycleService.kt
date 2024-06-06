/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.opentelemetry.android.internal.services.Startable

class AppLifecycleService internal constructor(
    private val applicationStateWatcher: ApplicationStateWatcher,
    private val appLifecycle: Lifecycle,
) : Startable {
    companion object {
        @JvmStatic
        fun create(): AppLifecycleService {
            return AppLifecycleService(
                ApplicationStateWatcher(),
                ProcessLifecycleOwner.get().lifecycle,
            )
        }
    }

    fun registerListener(listener: ApplicationStateListener) {
        applicationStateWatcher.registerListener(listener)
    }

    override fun start() {
        appLifecycle.addObserver(applicationStateWatcher)
    }
}
