/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.view.MotionEvent
import android.view.Window.Callback

class WindowCallbackWrapper(
    private val callback: Callback,
    private val viewClickEventGenerator: ViewClickEventGenerator,
) : Callback by callback {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        viewClickEventGenerator.generateClick(event)
        return callback.dispatchTouchEvent(event)
    }

    fun unwrap(): Callback = callback
}
