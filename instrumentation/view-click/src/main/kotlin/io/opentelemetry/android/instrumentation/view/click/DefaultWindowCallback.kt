/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.view.ActionMode
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.Window.Callback

abstract class DefaultWindowCallback(
    private val callback: Callback,
) : Callback by callback {
    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean = false

    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int,
    ): ActionMode? = null

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        ViewClickEventGenerator.generateClick(event)
        return callback.dispatchTouchEvent(event)
    }

    fun unwrap(): Callback = callback
}
