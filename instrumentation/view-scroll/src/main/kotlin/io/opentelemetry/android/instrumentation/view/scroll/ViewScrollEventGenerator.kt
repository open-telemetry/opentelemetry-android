/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.scroll

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.Window
import io.opentelemetry.android.instrumentation.view.common.Gesture
import io.opentelemetry.android.instrumentation.view.common.ToggleableTracker
import io.opentelemetry.android.instrumentation.view.common.TouchEventConsumer
import io.opentelemetry.android.instrumentation.view.common.WindowCallbackWrapper
import io.opentelemetry.android.instrumentation.view.common.findTargetForTap
import io.opentelemetry.android.instrumentation.view.common.viewToName
import io.opentelemetry.android.semconv.events.AppScreenFlingEvent
import io.opentelemetry.android.semconv.events.AppScreenScrollEvent
import io.opentelemetry.android.semconv.events.AppWidgetFlingEvent
import io.opentelemetry.android.semconv.events.AppWidgetScrollEvent
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.kotlin.semconv.IncubatingApi
import java.lang.ref.WeakReference

internal class ViewScrollEventGenerator(
    private val eventLogger: Logger,
) : TouchEventConsumer,
    ToggleableTracker {
    private var windowRef: WeakReference<Window>? = null

    @OptIn(IncubatingApi::class)
    private val gestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
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

                    findTargetForTap(window.decorView, safeEvent.x, safeEvent.y, viewCoordinates)?.let { view ->
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

                    findTargetForTap(window.decorView, safeEvent.x, safeEvent.y, viewCoordinates)?.let { view ->
                        emitAppWidgetFling(view, gesture)
                    }
                    safeEvent.recycle()
                }
                return false
            }
        }

    val gestureDetector = GestureDetector(null, gestureListener)

    private val viewCoordinates = IntArray(2)

    override fun startTracking(window: Window) {
        windowRef = WeakReference(window)
        val currentCallback = window.callback
        window.callback = WindowCallbackWrapper(currentCallback, this)
    }

    override fun consumeEvent(event: MotionEvent?) {
        if (event != null) {
            gestureDetector.onTouchEvent(event)
        }
    }

    override fun stopTracking() {
        windowRef?.get()?.run {
            if (callback is WindowCallbackWrapper) {
                callback = (callback as WindowCallbackWrapper).unwrap()
            }
        }
        windowRef = null
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
}
