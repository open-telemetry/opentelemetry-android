/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import io.opentelemetry.android.instrumentation.view.click.internal.Gesture
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_BUTTON
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_CLICKS
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_TYPE
import io.opentelemetry.android.semconv.events.AppScreenFlingEvent
import io.opentelemetry.android.semconv.events.AppScreenLongpressEvent
import io.opentelemetry.android.semconv.events.AppScreenScrollEvent
import io.opentelemetry.android.semconv.events.AppWidgetFlingEvent
import io.opentelemetry.android.semconv.events.AppWidgetLongpressEvent
import io.opentelemetry.android.semconv.events.AppWidgetScrollEvent
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.kotlin.attributes.AttributesMutator
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.events.AppScreenClickEvent
import io.opentelemetry.kotlin.semconv.events.AppWidgetClickEvent
import java.lang.ref.WeakReference
import java.util.LinkedList
import io.opentelemetry.kotlin.logging.Logger as KotlinLogger

@OptIn(IncubatingApi::class)
internal class ViewClickEventGenerator(
    private val eventLogger: Logger,
    private val clickEventLogger: KotlinLogger,
) {
    private var windowRef: WeakReference<Window>? = null

    private val gestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
                windowRef?.get()?.let { window ->

                    val safeEvent = MotionEvent.obtain(motionEvent)
                    val gesture = Gesture.Click(motionEvent, 2)

                    emitAppScreenClick(safeEvent, gesture, 2)

                    findTargetForTap(window.decorView, safeEvent.x, safeEvent.y)?.let { view ->
                        emitAppWidgetClick(view, gesture, 2)
                    }

                    safeEvent.recycle()
                }
                return false
            }

            override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
                windowRef?.get()?.let { window ->

                    val safeEvent = MotionEvent.obtain(motionEvent)
                    val gesture = Gesture.Click(safeEvent, 1)

                    emitAppScreenClick(safeEvent, gesture, 1)

                    findTargetForTap(window.decorView, safeEvent.x, safeEvent.y)?.let { view ->
                        emitAppWidgetClick(view, gesture, 1)
                    }
                    safeEvent.recycle()
                }
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                windowRef?.get()?.let { window ->

                    val safeEvent = MotionEvent.obtain(e)
                    val gesture = Gesture.LongPress(safeEvent)
                    emitAppScreenLongPress(safeEvent, gesture)

                    findTargetForTap(window.decorView, safeEvent.x, safeEvent.y)?.let { view ->
                        emitAppWidgetLongPress(view, gesture)
                    }
                    safeEvent.recycle()
                }
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                windowRef?.get()?.let { window ->

                    val safeEvent = MotionEvent.obtain(e2)
                    val gesture = Gesture.Scroll(safeEvent, distanceX.toDouble(), distanceY.toDouble())
                    emitAppScreenScroll(safeEvent, gesture)

                    findTargetForTap(window.decorView, safeEvent.x, safeEvent.y)?.let { view ->
                        emitAppWidgetScroll(view, gesture)
                    }
                    safeEvent.recycle()
                }
                return false
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                windowRef?.get()?.let { window ->

                    val safeEvent = MotionEvent.obtain(e2)
                    val gesture = Gesture.Fling(safeEvent, velocityX.toDouble(), velocityY.toDouble())
                    emitAppScreenFling(safeEvent, gesture)

                    findTargetForTap(window.decorView, safeEvent.x, safeEvent.y)?.let { view ->
                        emitAppWidgetFling(view, gesture)
                    }
                    safeEvent.recycle()
                }
                return false
            }
        }

    val gestureDetector = GestureDetector(null, gestureListener)

    private val viewCoordinates = IntArray(2)

    fun startTracking(window: Window) {
        windowRef = WeakReference(window)
        val currentCallback = window.callback
        window.callback = WindowCallbackWrapper(currentCallback, this)
    }

    fun generateClick(motionEvent: MotionEvent?) {
        if (motionEvent != null) {
            gestureDetector.onTouchEvent(motionEvent)
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

    private fun emitAppScreenClick(
        motionEvent: MotionEvent,
        gesture: Gesture.Click,
        clicks: Long,
    ) {
        AppScreenClickEvent(
            appScreenCoordinateX = motionEvent.x.toLong(),
            appScreenCoordinateY = motionEvent.y.toLong(),
        ).emit(clickEventLogger) {
            setPointerAttributes(gesture, clicks)
        }
    }

    private fun emitAppWidgetClick(
        view: View,
        gesture: Gesture.Click,
        clicks: Long,
    ) {
        AppWidgetClickEvent(
            appScreenCoordinateX = view.x.toLong(),
            appScreenCoordinateY = view.y.toLong(),
            appWidgetId = view.id.toString(),
            appWidgetName = viewToName(view),
        ).emit(clickEventLogger) {
            setPointerAttributes(gesture, clicks)
        }
    }

    private fun AttributesMutator.setPointerAttributes(
        gesture: Gesture.Click,
        clicks: Long,
    ) {
        setStringAttribute(HW_POINTER_TYPE, gesture.toolTypeDescription)
        gesture.buttonStateDescription?.let { setStringAttribute(HW_POINTER_BUTTON, it) }
        setLongAttribute(HW_POINTER_CLICKS, clicks)
    }

    private fun emitAppScreenLongPress(
        motionEvent: MotionEvent,
        gesture: Gesture.LongPress,
    ) {
        AppScreenLongpressEvent(
            appScreenCoordinateX = motionEvent.x.toLong(),
            appScreenCoordinateY = motionEvent.y.toLong(),
            hwPointerButton = gesture.buttonStateDescription,
            hwPointerType = gesture.toolTypeDescription,
        ).emit(eventLogger)
    }

    private fun emitAppWidgetLongPress(
        view: View,
        gesture: Gesture.LongPress,
    ) {
        AppWidgetLongpressEvent(
            appScreenCoordinateX = view.x.toLong(),
            appScreenCoordinateY = view.y.toLong(),
            appWidgetId = view.id.toString(),
            appWidgetName = viewToName(view),
            hwPointerButton = gesture.buttonStateDescription,
            hwPointerType = gesture.toolTypeDescription,
        ).emit(eventLogger)
    }

    private fun emitAppScreenScroll(
        motionEvent: MotionEvent,
        gesture: Gesture.Scroll,
    ) {
        AppScreenScrollEvent(
            appScreenCoordinateX = motionEvent.x.toLong(),
            appScreenCoordinateY = motionEvent.y.toLong(),
            hwPointerButton = gesture.buttonStateDescription,
            hwPointerDistanceX = gesture.distanceX,
            hwPointerDistanceY = gesture.distanceY,
            hwPointerType = gesture.toolTypeDescription,
        ).emit(eventLogger)
    }

    private fun emitAppWidgetScroll(
        view: View,
        gesture: Gesture.Scroll,
    ) {
        AppWidgetScrollEvent(
            appScreenCoordinateX = view.x.toLong(),
            appScreenCoordinateY = view.y.toLong(),
            appWidgetId = view.id.toString(),
            appWidgetName = viewToName(view),
            hwPointerButton = gesture.buttonStateDescription,
            hwPointerDistanceX = gesture.distanceX,
            hwPointerDistanceY = gesture.distanceY,
            hwPointerType = gesture.toolTypeDescription,
        ).emit(eventLogger)
    }

    private fun emitAppScreenFling(
        motionEvent: MotionEvent,
        gesture: Gesture.Fling,
    ) {
        AppScreenFlingEvent(
            appScreenCoordinateX = motionEvent.x.toLong(),
            appScreenCoordinateY = motionEvent.y.toLong(),
            hwPointerButton = gesture.buttonStateDescription,
            hwPointerType = gesture.toolTypeDescription,
            hwPointerVelocityX = gesture.velocityX,
            hwPointerVelocityY = gesture.velocityY,
        ).emit(eventLogger)
    }

    private fun emitAppWidgetFling(
        view: View,
        gesture: Gesture.Fling,
    ) {
        AppWidgetFlingEvent(
            appScreenCoordinateX = view.x.toLong(),
            appScreenCoordinateY = view.y.toLong(),
            appWidgetId = view.id.toString(),
            appWidgetName = viewToName(view),
            hwPointerButton = gesture.buttonStateDescription,
            hwPointerType = gesture.toolTypeDescription,
            hwPointerVelocityX = gesture.velocityX,
            hwPointerVelocityY = gesture.velocityY,
        ).emit(eventLogger)
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
