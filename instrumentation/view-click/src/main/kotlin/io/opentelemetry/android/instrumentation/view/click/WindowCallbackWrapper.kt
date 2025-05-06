/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.os.Build.VERSION_CODES
import android.view.ActionMode
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.Window.Callback
import androidx.annotation.RequiresApi

class WindowCallbackWrapper(
    private val callback: Callback,
    private val viewClickEventGenerator: ViewClickEventGenerator,
) : Callback by callback {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        viewClickEventGenerator.generateClick(event)
        return callback.dispatchTouchEvent(event)
    }

    @RequiresApi(api = VERSION_CODES.M)
    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean = callback.onSearchRequested(searchEvent)

    @RequiresApi(api = VERSION_CODES.M)
    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int,
    ): ActionMode? = this.callback.onWindowStartingActionMode(callback, type)

    fun unwrap(): Callback = callback
}
