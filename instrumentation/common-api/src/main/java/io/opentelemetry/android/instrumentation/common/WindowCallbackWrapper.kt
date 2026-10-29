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

class WindowCallbackWrapper private constructor(
    private val callback: Callback,
) : Callback by callback {
    private val callbackMap = LinkedHashMap<String, TouchEventConsumer>()

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        val consumers = callbackMap.values.toList()
        for (touchConsumer in consumers) {
            touchConsumer.consumeEvent(event)
        }
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

    fun keys(): List<String> = callbackMap.keys.toList()

    companion object {
        private val windowMap = SmallMap<Window, WindowCallbackWrapper>()
        private const val MAX_ENTRIES = 2

        class SmallMap<K, V> : LinkedHashMap<K, V>() {
            override fun removeEldestEntry(eldest: Map.Entry<K?, V?>?): Boolean = size >= MAX_ENTRIES
        }

        @JvmStatic
        fun registerConsumer(
            window: Window,
            key: String,
            consumer: TouchEventConsumer,
        ) {
            if (windowMap[window] == null) {
                windowMap[window] = WindowCallbackWrapper(window.callback)
                window.callback = windowMap[window]!!
            }
            val wrapper = windowMap[window]!!
            val callbackMap = wrapper.callbackMap
            if (callbackMap[key] != null) {
                callbackMap.remove(key)
            }
            callbackMap[key] = consumer
        }

        @JvmStatic
        fun unregisterConsumer(
            window: Window,
            key: String,
        ) {
            val wrapper = windowMap[window] ?: return
            wrapper.callbackMap.remove(key)
            if (wrapper.callbackMap.isEmpty()) {
                windowMap.remove(window)
                if (window.callback is WindowCallbackWrapper) {
                    window.callback = wrapper.unwrap()
                }
            }
        }
    }
}

interface TouchEventConsumer {
    fun consumeEvent(motionEvent: MotionEvent?)
}
