/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.Window.Callback
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class Pre23WindowCallbackWrapper(
    private val callback: Callback,
) : DefaultWindowCallback {
    override fun onSearchRequested(): Boolean = callback.onSearchRequested()

    override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? =
        this.callback.onWindowStartingActionMode(callback)

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean = callback.dispatchKeyEvent(event)

    override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean = callback.dispatchKeyShortcutEvent(event)

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        ViewClickEventGenerator.generateClick(event)
        return callback.dispatchTouchEvent(event)
    }

    override fun dispatchTrackballEvent(event: MotionEvent?): Boolean = callback.dispatchTrackballEvent(event)

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean = callback.dispatchGenericMotionEvent(event)

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean =
        callback.dispatchPopulateAccessibilityEvent(event)

    override fun onCreatePanelView(featureId: Int): View? = callback.onCreatePanelView(featureId)

    override fun onCreatePanelMenu(
        featureId: Int,
        menu: Menu,
    ): Boolean = callback.onCreatePanelMenu(featureId, menu)

    override fun onPreparePanel(
        featureId: Int,
        view: View?,
        menu: Menu,
    ): Boolean = callback.onPreparePanel(featureId, view, menu)

    override fun onMenuOpened(
        featureId: Int,
        menu: Menu,
    ): Boolean = callback.onMenuOpened(featureId, menu)

    override fun onMenuItemSelected(
        featureId: Int,
        item: MenuItem,
    ): Boolean = callback.onMenuItemSelected(featureId, item)

    override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) {
        callback.onWindowAttributesChanged(attrs)
    }

    override fun onContentChanged() {
        callback.onContentChanged()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        callback.onWindowFocusChanged(hasFocus)
    }

    override fun onAttachedToWindow() {
        callback.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        callback.onDetachedFromWindow()
    }

    override fun onPanelClosed(
        featureId: Int,
        menu: Menu,
    ) {
        callback.onPanelClosed(featureId, menu)
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        callback.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        callback.onActionModeFinished(mode)
    }

    fun unwrap(): Callback = callback
}
