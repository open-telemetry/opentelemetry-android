/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.ActionMode
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.Window
import androidx.annotation.RequiresApi
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

internal fun registerSessionInputActivityTracker(
    context: Context,
    recordActivity: () -> Unit,
) {
    (context as? Application)?.registerActivityLifecycleCallbacks(
        SessionInputActivityTracker(recordActivity),
    )
}

internal class SessionInputActivityTracker(
    private val recordActivity: () -> Unit,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        val window = activity.window
        val callback = window.callback
        if (callback !is SessionInputWindowCallback) {
            window.callback = SessionInputWindowCallback(callback, recordActivity)
        }
    }
}

internal class SessionInputWindowCallback(
    private val callback: Window.Callback,
    private val recordActivity: () -> Unit,
) : Window.Callback by callback {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event?.actionMasked == MotionEvent.ACTION_DOWN) {
            recordActivitySafely()
        }
        return callback.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            recordActivitySafely()
        }
        return callback.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event?.actionMasked == MotionEvent.ACTION_SCROLL) {
            recordActivitySafely()
        }
        return callback.dispatchGenericMotionEvent(event)
    }

    private fun recordActivitySafely() {
        runCatching(recordActivity)
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
}
