/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
internal class ApplicationStateWatcher :
    DefaultLifecycleObserver,
    Closeable {
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

    override fun close() {
        applicationStateListeners.clear()
    }
}
