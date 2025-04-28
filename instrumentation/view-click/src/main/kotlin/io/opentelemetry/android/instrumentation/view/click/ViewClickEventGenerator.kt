/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import io.opentelemetry.android.instrumentation.view.click.internal.APP_SCREEN_CLICK_EVENT_NAME
import io.opentelemetry.android.instrumentation.view.click.internal.VIEW_CLICK_EVENT_NAME
import io.opentelemetry.android.instrumentation.view.click.internal.viewIdAttr
import io.opentelemetry.android.instrumentation.view.click.internal.viewNameAttr
import io.opentelemetry.android.instrumentation.view.click.internal.xCoordinateAttr
import io.opentelemetry.android.instrumentation.view.click.internal.yCoordinateAttr
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder
import io.opentelemetry.api.incubator.logs.ExtendedLogger
import java.lang.ref.WeakReference
import java.util.LinkedList

class ViewClickEventGenerator(
    private val eventLogger: ExtendedLogger,
) {
    private var windowRef: WeakReference<Window>? = null

    private val viewCoordinates = IntArray(2)

    fun startTracking(window: Window) {
        windowRef = WeakReference(window)
        val currentCallback = window.callback
        window.callback = WindowCallbackWrapper(currentCallback, this)
    }

    fun generateClick(motionEvent: MotionEvent?) {
        windowRef?.get()?.let { window ->
            if (motionEvent != null && motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                createEvent(APP_SCREEN_CLICK_EVENT_NAME)
                    .setAttribute(yCoordinateAttr, motionEvent.y.toDouble())
                    .setAttribute(xCoordinateAttr, motionEvent.x.toDouble())
                    .emit()

                findTargetForTap(window.decorView, motionEvent.x, motionEvent.y)?.let { view ->
                    createEvent(VIEW_CLICK_EVENT_NAME)
                        .setAllAttributes(createViewAttributes(view))
                        .emit()
                }
            }
        }
    }

    fun stopTracking() {
        windowRef?.get()?.run {
            if (callback is WindowCallbackWrapper) {
                callback = (callback as WindowCallbackWrapper).unwrap()
            }
        }
        windowRef = null
    }

    private fun createEvent(name: String): ExtendedLogRecordBuilder =
        eventLogger
            .logRecordBuilder()
            .setEventName(name)

    private fun createViewAttributes(view: View): Attributes {
        val builder = Attributes.builder()
        builder.put(viewNameAttr, viewToName(view))
        builder.put(viewIdAttr, view.id.toLong())

        builder.put(xCoordinateAttr, view.x.toDouble())
        builder.put(yCoordinateAttr, view.y.toDouble())
        return builder.build()
    }

    private fun viewToName(view: View): String =
        try {
            view.resources?.getResourceEntryName(view.id) ?: view.id.toString()
        } catch (throwable: Throwable) {
            view.id.toString()
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

    private val View.isVisible: Boolean
        get() = visibility == View.VISIBLE
}
