/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common

import android.os.Build.VERSION_CODES
import android.view.ActionMode
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.Window
import android.view.Window.Callback
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference

class WindowCallbackWrapperClick(
    private val callback: Callback,
    private val consumer: TouchEventConsumer,
) : Callback by callback {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        consumer.consumeEvent(event)
        return callback.dispatchTouchEvent(event)
    }

    @RequiresApi(api = VERSION_CODES.O)
    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        callback.onPointerCaptureChanged(hasCapture)
    }

    @RequiresApi(api = VERSION_CODES.N)
    override fun onProvideKeyboardShortcuts(
        data: List<KeyboardShortcutGroup?>?,
        menu: Menu?,
        deviceId: Int,
    ) {
        callback.onProvideKeyboardShortcuts(data, menu, deviceId)
    }

    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean = callback.onSearchRequested(searchEvent)

    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int,
    ): ActionMode? = this.callback.onWindowStartingActionMode(callback, type)

    fun unwrap(): Callback = callback
}

class WindowCallbackWrapperCompose(
    private val callback: Callback,
    private val consumer: TouchEventConsumer,
) : Callback by callback {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        consumer.consumeEvent(event)
        return callback.dispatchTouchEvent(event)
    }

    @RequiresApi(api = VERSION_CODES.O)
    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        callback.onPointerCaptureChanged(hasCapture)
    }

    @RequiresApi(api = VERSION_CODES.N)
    override fun onProvideKeyboardShortcuts(
        data: List<KeyboardShortcutGroup?>?,
        menu: Menu?,
        deviceId: Int,
    ) {
        callback.onProvideKeyboardShortcuts(data, menu, deviceId)
    }

    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean = callback.onSearchRequested(searchEvent)

    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int,
    ): ActionMode? = this.callback.onWindowStartingActionMode(callback, type)

    fun unwrap(): Callback = callback
}

class MinimalTrackerCompose :
    TouchEventConsumer,
    ToggleableTracker {
    private var windowRef: WeakReference<Window>? = null

    override fun startTracking(window: Window) {
        windowRef = WeakReference(window)
        val currentCallback = window.callback
        window.callback = WindowCallbackWrapperCompose(currentCallback, this)
    }

    override fun consumeEvent(event: MotionEvent?) {
        windowRef?.get()?.let { window ->
        }
    }

    override fun stopTracking() {
        windowRef?.get()?.run {
            if (callback is WindowCallbackWrapperCompose) {
                callback = (callback as WindowCallbackWrapperCompose).unwrap()
            }
        }
        windowRef = null
    }
}

class MinimalTrackerClick :
    TouchEventConsumer,
    ToggleableTracker {
    private var windowRef: WeakReference<Window>? = null

    override fun startTracking(window: Window) {
        windowRef = WeakReference(window)
        val currentCallback = window.callback
        window.callback = WindowCallbackWrapperClick(currentCallback, this)
    }

    override fun consumeEvent(event: MotionEvent?) {
        windowRef?.get()?.let { window ->
        }
    }

    override fun stopTracking() {
        windowRef?.get()?.run {
            if (callback is WindowCallbackWrapperClick) {
                callback = (callback as WindowCallbackWrapperClick).unwrap()
            }
        }
        windowRef = null
    }
}

class ThirdPartyWindowCallbackWrapper(
    private val callback: Callback,
    private val consumer: TouchEventConsumer,
) : Callback by callback {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        consumer.consumeEvent(event)
        return callback.dispatchTouchEvent(event)
    }

    @RequiresApi(api = VERSION_CODES.O)
    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        callback.onPointerCaptureChanged(hasCapture)
    }

    @RequiresApi(api = VERSION_CODES.N)
    override fun onProvideKeyboardShortcuts(
        data: List<KeyboardShortcutGroup?>?,
        menu: Menu?,
        deviceId: Int,
    ) {
        callback.onProvideKeyboardShortcuts(data, menu, deviceId)
    }

    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean = callback.onSearchRequested(searchEvent)

    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int,
    ): ActionMode? = this.callback.onWindowStartingActionMode(callback, type)

    fun unwrap(): Callback = callback
}

class MinimalTrackerThirdParty :
    TouchEventConsumer,
    ToggleableTracker {
    private var windowRef: WeakReference<Window>? = null

    override fun startTracking(window: Window) {
        windowRef = WeakReference(window)
        val currentCallback = window.callback
        window.callback = ThirdPartyWindowCallbackWrapper(currentCallback, this)
    }

    override fun consumeEvent(event: MotionEvent?) {
        windowRef?.get()?.let { window ->
        }
    }

    override fun stopTracking() {
    }
}
