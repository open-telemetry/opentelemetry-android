/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

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
import io.opentelemetry.android.semconv.events.AppScreenLongpressEvent
import io.opentelemetry.android.semconv.events.AppWidgetLongpressEvent
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_COORDINATE_X
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_COORDINATE_Y
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_WIDGET_ID
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_WIDGET_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi
import java.lang.ref.WeakReference

internal class ViewClickEventGenerator(
    private val eventLogger: Logger,
) : TouchEventConsumer,
    ToggleableTracker {
    private var windowRef: WeakReference<Window>? = null

    @OptIn(IncubatingApi::class)
    private val gestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
                windowRef?.get()?.let { window ->

                    val safeEvent = MotionEvent.obtain(motionEvent)
                    val gesture = Gesture.Click(motionEvent, 2)

                    createEvent(APP_SCREEN_CLICK_EVENT_NAME, gesture)
                        .setAttribute(APP_SCREEN_COORDINATE_Y, safeEvent.y.toLong())
                        .setAttribute(APP_SCREEN_COORDINATE_X, safeEvent.x.toLong())
                        .emit()

                    findTargetForTap(window.decorView, safeEvent.x, safeEvent.y, viewCoordinates)?.let { view ->
                        createEvent(VIEW_CLICK_EVENT_NAME, gesture)
                            .setAllAttributes(createViewAttributes(view))
                            .emit()
                    }

                    safeEvent.recycle()
                }
                return false
            }

            override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
                windowRef?.get()?.let { window ->

                    val safeEvent = MotionEvent.obtain(motionEvent)
                    val gesture = Gesture.Click(safeEvent, 1)

                    createEvent(APP_SCREEN_CLICK_EVENT_NAME, gesture)
                        .setAttribute(APP_SCREEN_COORDINATE_Y, safeEvent.y.toLong())
                        .setAttribute(APP_SCREEN_COORDINATE_X, safeEvent.x.toLong())
                        .emit()

                    findTargetForTap(window.decorView, safeEvent.x, safeEvent.y, viewCoordinates)?.let { view ->
                        createEvent(VIEW_CLICK_EVENT_NAME, gesture)
                            .setAllAttributes(createViewAttributes(view))
                            .emit()
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

                    findTargetForTap(window.decorView, safeEvent.x, safeEvent.y, viewCoordinates)?.let { view ->
                        emitAppWidgetLongPress(view, gesture)
                    }
                    safeEvent.recycle()
                }
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

    private fun createEvent(
        name: String,
        gesture: Gesture,
    ): LogRecordBuilder {
        val logger =
            eventLogger
                .logRecordBuilder()
                .setEventName(name)
                .setAllAttributes(gesture.attributes)

        return logger
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

internal const val APP_SCREEN_CLICK_EVENT_NAME = "app.screen.click"
internal const val VIEW_CLICK_EVENT_NAME = "app.widget.click"
