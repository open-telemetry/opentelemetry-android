/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.os.Build.VERSION_CODES
import android.view.ActionMode
import android.view.SearchEvent
import android.view.Window.Callback
import androidx.annotation.RequiresApi

@RequiresApi(api = VERSION_CODES.M)
class WindowCallbackWrapper(
    private val callback: Callback,
) : DefaultWindowCallback(callback) {
    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean = callback.onSearchRequested(searchEvent)

    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int,
    ): ActionMode? = this.callback.onWindowStartingActionMode(callback, type)
}
