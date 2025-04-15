/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.os.Build.VERSION_CODES
import android.view.MotionEvent
import android.view.Window.Callback
import androidx.annotation.RequiresApi

@RequiresApi(api = VERSION_CODES.M)
class WindowCallbackWrapper(
    private val callback: Callback,
) : Callback by callback {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        ViewClickEventGenerator.generateClick(event)
        return callback.dispatchTouchEvent(event)
    }
}
