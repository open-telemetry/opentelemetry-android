/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import androidx.lifecycle.Lifecycle

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
class AppLifecycle internal constructor(
    private val applicationStateWatcher: ApplicationStateWatcher,
    appLifecycle: Lifecycle,
) {
    init {
        appLifecycle.addObserver(applicationStateWatcher)
    }

    fun registerListener(listener: ApplicationStateListener) {
        applicationStateWatcher.registerListener(listener)
    }
}
