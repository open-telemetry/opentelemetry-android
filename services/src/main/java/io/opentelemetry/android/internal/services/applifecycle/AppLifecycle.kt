/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import androidx.lifecycle.Lifecycle
import io.opentelemetry.android.internal.services.Service

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
class AppLifecycle internal constructor(
    private val applicationStateWatcher: ApplicationStateWatcher,
    appLifecycle: Lifecycle,
) : Service {
    init {
        appLifecycle.addObserver(applicationStateWatcher)
    }

    fun registerListener(listener: ApplicationStateListener) {
        applicationStateWatcher.registerListener(listener)
    }
}
