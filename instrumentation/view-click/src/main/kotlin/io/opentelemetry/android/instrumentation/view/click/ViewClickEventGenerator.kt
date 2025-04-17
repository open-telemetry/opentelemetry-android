/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.os.Build
import android.os.Build.VERSION_CODES
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import java.lang.ref.WeakReference
import java.util.LinkedList

internal object ViewClickEventGenerator {
    private var windowRef: WeakReference<Window>? = null

    private val viewCoordinates = IntArray(2)

    fun startTracking(window: Window) {
        windowRef = WeakReference(window)
        val currentCallback = window.callback
        val newCallback =
            if (Build.VERSION.SDK_INT < VERSION_CODES.M) {
                Pre23WindowCallbackWrapper(currentCallback)
            } else {
                WindowCallbackWrapper(currentCallback)
            }

        window.callback = newCallback
    }

    fun generateClick(motionEvent: MotionEvent?) {
        windowRef?.get()?.let { window ->
            if (motionEvent != null && motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                EventBuilderCreator
                    .createEvent(APP_SCREEN_CLICK_EVENT_NAME)
                    .setAttribute(yCoordinateAttr, motionEvent.y.toDouble())
                    .setAttribute(xCoordinateAttr, motionEvent.x.toDouble())
                    .emit()

                findTargetForTap(window.decorView, motionEvent.x, motionEvent.y)?.let { view ->
                    EventBuilderCreator
                        .createEvent(VIEW_CLICK_EVENT_NAME)
                        .setAllAttributes(EventBuilderCreator.createViewAttributes(view))
                        .emit()
                }
            }
        }
    }

    fun stopTracking() {
        windowRef?.get()?.run {
            if (callback is DefaultWindowCallback) {
                callback = (callback as DefaultWindowCallback).unwrap()
            }
        }
        windowRef = null
    }

    private fun findTargetForTap(
        decorView: View,
        x: Float,
        y: Float,
    ): View? {
        val queue = LinkedList<View>()
        queue.addFirst(decorView)
        var target: View? = null

        while (queue.isNotEmpty()) {
            val view = queue.removeFirst()
            if (isJetpackComposeView(view)) {
                return null
            }

            if (isValidClickTarget(view)) {
                target = view
            }

            if (view is ViewGroup) {
                handleViewGroup(view, x, y, queue)
            }
        }
        return target
    }

    private fun isValidClickTarget(view: View): Boolean = view.isClickable && view.isVisible

    private fun handleViewGroup(
        view: ViewGroup,
        x: Float,
        y: Float,
        stack: LinkedList<View>,
    ) {
        if (!view.isVisible) return

        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (hitTest(child, x, y) && !isJetpackComposeView(child)) {
                stack.add(child)
            }
        }
    }

    private fun hitTest(
        view: View,
        x: Float,
        y: Float,
    ): Boolean {
        view.getLocationInWindow(viewCoordinates)
        val vx = viewCoordinates[0]
        val vy = viewCoordinates[1]

        val w = view.width
        val h = view.height
        return !(x < vx || x > vx + w || y < vy || y > vy + h)
    }

    private fun isJetpackComposeView(view: View): Boolean = view::class.java.name.startsWith("androidx.compose.ui.platform.ComposeView")
}
