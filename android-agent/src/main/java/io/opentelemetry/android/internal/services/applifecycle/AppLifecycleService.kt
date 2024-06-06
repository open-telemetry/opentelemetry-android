/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import androidx.lifecycle.ProcessLifecycleOwner
import io.opentelemetry.android.internal.services.Startable

class AppLifecycleService {
    class AppLifecycleService internal constructor(
        private val applicationStateWatcher: ApplicationStateWatcher,
    ) : Startable {
        companion object {
            @JvmStatic
            fun create(): AppLifecycleService {
                return AppLifecycleService(ApplicationStateWatcher())
            }
        }

        fun registerListener(listener: ApplicationStateListener) {
            applicationStateWatcher.registerListener(listener)
        }

        override fun start() {
            ProcessLifecycleOwner.get().lifecycle.addObserver(applicationStateWatcher)
        }
    }
}
