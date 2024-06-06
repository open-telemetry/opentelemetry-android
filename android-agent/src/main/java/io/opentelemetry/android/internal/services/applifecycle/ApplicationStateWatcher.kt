/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.CopyOnWriteArrayList

internal class ApplicationStateWatcher : DefaultLifecycleObserver {
    private val applicationStateListeners: MutableList<ApplicationStateListener> =
        CopyOnWriteArrayList()

    override fun onStart(owner: LifecycleOwner) {
        for (listener in applicationStateListeners) {
            listener.onApplicationForegrounded()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        for (listener in applicationStateListeners) {
            listener.onApplicationBackgrounded()
        }
    }

    fun registerListener(listener: ApplicationStateListener) {
        applicationStateListeners.add(listener)
    }
}
