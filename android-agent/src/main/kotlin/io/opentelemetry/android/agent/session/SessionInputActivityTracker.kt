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
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks
import java.util.WeakHashMap

internal class SessionInputActivityInstrumentation(
    private val recordActivity: () -> Unit,
) : AndroidInstrumentation {
    override val name: String = "session-input-activity"

    private var tracker: SessionInputActivityTracker? = null

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val application = context as? Application ?: return
        SessionInputActivityTracker(recordActivity).also {
            tracker = it
            application.registerActivityLifecycleCallbacks(it)
        }
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val application = context as? Application ?: return
        tracker?.let {
            application.unregisterActivityLifecycleCallbacks(it)
            it.close()
        }
        tracker = null
    }
}

internal class SessionInputActivityTracker(
    private val recordActivity: () -> Unit,
) : DefaultingActivityLifecycleCallbacks {
    private val trackedWindows = WeakHashMap<Window, SessionInputWindowCallback>()

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        track(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        track(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        synchronized(trackedWindows) {
            untrack(activity.window, trackedWindows.remove(activity.window))
        }
    }

    private fun track(activity: Activity) {
        synchronized(trackedWindows) {
            val window = activity.window
            if (trackedWindows.containsKey(window)) {
                return
            }
            val callback = window.callback
            if (callback !is SessionInputWindowCallback) {
                SessionInputWindowCallback(callback, recordActivity).also {
                    window.callback = it
                    trackedWindows[window] = it
                }
            }
        }
    }

    fun close() {
        synchronized(trackedWindows) {
            trackedWindows.forEach { (window, callback) ->
                untrack(window, callback)
            }
            trackedWindows.clear()
        }
    }

    private fun untrack(
        window: Window,
        callback: SessionInputWindowCallback?,
    ) {
        callback ?: return
        if (window.callback === callback) {
            window.callback = callback.unwrap()
        }
        callback.detach()
    }
}

internal class SessionInputWindowCallback(
    private val callback: Window.Callback,
    recordActivity: () -> Unit,
) : Window.Callback by callback {
    @Volatile
    private var activityRecorder: (() -> Unit)? = recordActivity

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
        activityRecorder?.let { runCatching(it) }
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

    fun unwrap(): Window.Callback = callback

    fun detach() {
        activityRecorder = null
    }
}
