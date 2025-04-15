/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.view.ActionMode
import android.view.SearchEvent
import android.view.Window

interface DefaultWindowCallback : Window.Callback {
    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean = false

    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int,
    ): ActionMode? = null
}
