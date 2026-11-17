/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.click

import android.os.Build.VERSION_CODES
import android.util.Log
import android.view.ActionMode
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.Window.Callback
import androidx.annotation.RequiresApi
import io.opentelemetry.android.common.RumConstants

internal class WindowCallbackWrapper(
    private val callback: Callback,
    private val composeClickEventGenerator: ComposeClickEventGenerator,
) : Callback by callback {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        try {
            composeClickEventGenerator.generateClick(event)
        } catch (e: Exception) {
            // click tracking must never break touch dispatch for the host app
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to generate click event", e)
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
}
