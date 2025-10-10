/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.draw

import android.os.Build
import android.view.ActionMode
import android.view.SearchEvent
import android.view.Window
import androidx.annotation.RequiresApi

internal fun Window.onDecorViewReady(callback: () -> Unit) {
    if (peekDecorView() == null) {
        onContentChanged {
            callback()
            return@onContentChanged false
        }
    } else {
        callback()
    }
}

internal fun Window.onContentChanged(callbackInvocation: () -> Boolean) {
    val currentCallback = callback
    val callback =
        if (currentCallback is WindowDelegateCallback) {
            currentCallback
        } else {
            val newCallback = WindowDelegateCallback(currentCallback)
            callback = newCallback
            newCallback
        }
    callback.onContentChangedCallbacks += callbackInvocation
}

internal class WindowDelegateCallback(
    private val delegate: Window.Callback,
) : Window.Callback by delegate {
    val onContentChangedCallbacks = mutableListOf<() -> Boolean>()

    override fun onContentChanged() {
        onContentChangedCallbacks.removeAll { callback ->
            !callback()
        }
        delegate.onContentChanged()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSearchRequested(searchEvent: SearchEvent): Boolean = delegate.onSearchRequested(searchEvent)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback,
        type: Int,
    ): ActionMode? = delegate.onWindowStartingActionMode(callback, type)
}
