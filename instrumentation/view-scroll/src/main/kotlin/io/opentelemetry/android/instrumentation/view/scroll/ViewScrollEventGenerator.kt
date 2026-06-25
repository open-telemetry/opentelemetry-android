/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.scroll

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.Window
import io.opentelemetry.android.instrumentation.internal.APP_SCREEN_FLING_EVENT_NAME
import io.opentelemetry.android.instrumentation.internal.APP_SCREEN_SCROLL_EVENT_NAME
import io.opentelemetry.android.instrumentation.internal.Gesture
import io.opentelemetry.android.instrumentation.internal.InternalViewApi
import io.opentelemetry.android.instrumentation.internal.VIEW_FLING_EVENT_NAME
import io.opentelemetry.android.instrumentation.internal.VIEW_SCROLL_EVENT_NAME
import io.opentelemetry.android.instrumentation.internal.findTargetForTap
import io.opentelemetry.android.instrumentation.internal.viewToName
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_COORDINATE_X
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_COORDINATE_Y
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_WIDGET_ID
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_WIDGET_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi
import java.lang.ref.WeakReference

@OptIn(InternalViewApi::class)
internal class ViewScrollEventGenerator(
    private val eventLogger: Logger,
) {
    private var windowRef: WeakReference<Window>? = null

    @OptIn(IncubatingApi::class)
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            windowRef?.get()?.let { window ->

                val safeEvent = MotionEvent.obtain(e2)
                val gesture = Gesture.Scroll(safeEvent, distanceX.toDouble(), distanceY.toDouble())
                createEvent(APP_SCREEN_SCROLL_EVENT_NAME, gesture)
                    .setAttribute(APP_SCREEN_COORDINATE_Y, safeEvent.y.toLong())
                    .setAttribute(APP_SCREEN_COORDINATE_X, safeEvent.x.toLong())
                    .emit()

                findTargetForTap(window.decorView, safeEvent.x, safeEvent.y, viewCoordinates)?.let { view ->
                    createEvent(VIEW_SCROLL_EVENT_NAME, gesture)
                        .setAllAttributes(createViewAttributes(view))
                        .emit()
                }
                safeEvent.recycle()
            }
            return false
        }

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
            windowRef?.get()?.let { window ->

                val safeEvent = MotionEvent.obtain(e2)
                val gesture = Gesture.Fling(safeEvent, velocityX.toDouble(), velocityY.toDouble())
                createEvent(APP_SCREEN_FLING_EVENT_NAME, gesture)
                    .setAttribute(APP_SCREEN_COORDINATE_Y, safeEvent.y.toLong())
                    .setAttribute(APP_SCREEN_COORDINATE_X, safeEvent.x.toLong())
                    .emit()

                findTargetForTap(window.decorView, safeEvent.x, safeEvent.y, viewCoordinates)?.let { view ->
                    createEvent(VIEW_FLING_EVENT_NAME, gesture)
                        .setAllAttributes(createViewAttributes(view))
                        .emit()
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

    fun generateTouchEvent(motionEvent: MotionEvent?) {
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

    private fun createEvent(name: String, gesture: Gesture): LogRecordBuilder {

        val logger = eventLogger
            .logRecordBuilder()
            .setEventName(name)
            .setAllAttributes(gesture.attributes)

        return logger
    }


    @OptIn(IncubatingApi::class)
    private fun createViewAttributes(view: View): Attributes {
        val builder = Attributes.builder()
        builder.put(APP_WIDGET_NAME, viewToName(view))
        builder.put(APP_WIDGET_ID, view.id.toString())

        builder.put(APP_SCREEN_COORDINATE_X, view.x.toLong())
        builder.put(APP_SCREEN_COORDINATE_Y, view.y.toLong())
        return builder.build()
    }

}
