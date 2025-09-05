/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import androidx.lifecycle.Lifecycle

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
internal class AppLifecycleImpl internal constructor(
    private val applicationStateWatcher: ApplicationStateWatcher,
    private val appLifecycle: Lifecycle,
) : AppLifecycle {
    init {
        appLifecycle.addObserver(applicationStateWatcher)
    }

    override fun registerListener(listener: ApplicationStateListener) {
        applicationStateWatcher.registerListener(listener)
    }

    override fun close() {
        appLifecycle.removeObserver(applicationStateWatcher)
        applicationStateWatcher.close()
    }
}
