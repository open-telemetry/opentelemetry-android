package io.opentelemetry.android.doubletap

import android.os.Build.VERSION_CODES
import android.view.ActionMode
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.Window.Callback
import androidx.annotation.RequiresApi

internal class DoubleTapWindowCallbackWrapper(
    private val callback: Callback,
    private val doubleTapEventGenerator: DoubleTapEventGenerator,
): Callback by callback {

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        doubleTapEventGenerator.generateDoubleTap(event)
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
