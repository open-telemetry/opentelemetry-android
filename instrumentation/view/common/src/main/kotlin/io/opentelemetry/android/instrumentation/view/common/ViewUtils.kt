/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.common

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import io.opentelemetry.api.common.Attributes
import java.util.LinkedList


const val APP_SCREEN_CLICK_EVENT_NAME = "app.screen.click"


const val VIEW_CLICK_EVENT_NAME = "app.widget.click"


const val APP_SCREEN_LONG_PRESS_EVENT_NAME = "app.screen.longpress"


const val VIEW_LONG_PRESS_EVENT_NAME = "app.widget.longpress"


const val HARDWARE_POINTER_TYPE = "hw.pointer.type"


const val HARDWARE_POINTER_BUTTON = "hw.pointer.button"


const val HARDWARE_POINTER_CLICKS = "hw.pointer.clicks"


const val APP_SCREEN_SCROLL_EVENT_NAME = "app.screen.scroll"


const val VIEW_SCROLL_EVENT_NAME = "app.widget.scroll"


const val APP_SCREEN_FLING_EVENT_NAME = "app.screen.fling"


const val VIEW_FLING_EVENT_NAME = "app.widget.fling"


const val HARDWARE_POINTER_VELOCITY_X = "hw.pointer.velocity.x"


const val HARDWARE_POINTER_VELOCITY_Y = "hw.pointer.velocity.y"



const val HARDWARE_POINTER_DISTANCE_X = "hw.pointer.distance.x"



const val HARDWARE_POINTER_DISTANCE_Y = "hw.pointer.distance.y"


fun toolTypeToString(toolTypeInt: Int): String {
    return when(toolTypeInt) {
        MotionEvent.TOOL_TYPE_MOUSE -> "mouse"
        MotionEvent.TOOL_TYPE_FINGER -> "finger"
        MotionEvent.TOOL_TYPE_STYLUS -> "stylus"
        MotionEvent.TOOL_TYPE_ERASER -> "eraser"
        else -> "unknown"
    }
}


sealed class Gesture(m: MotionEvent) {
    private val t = TapEventMetadata(m)
    var attributes = Attributes.empty()
        protected set

    init {
        attributes = attributes.toBuilder().put(HARDWARE_POINTER_TYPE, t.toolTypeDescription).build()
        if(t.buttonStateDescription != null) {
            attributes = attributes.toBuilder().put(HARDWARE_POINTER_BUTTON, t.buttonStateDescription).build()
        }
    }
    class LongPress(motionEvent: MotionEvent): Gesture(motionEvent)
    class Click(motionEvent: MotionEvent, clicks: Int): Gesture(motionEvent) {
        init {
            attributes = attributes.toBuilder().put(HARDWARE_POINTER_CLICKS, clicks.toLong()).build()
        }
    }
    class Fling(motionEvent: MotionEvent, velocityX: Double, velocityY: Double): Gesture(motionEvent) {
        init {
            attributes = attributes.toBuilder()
                .put(HARDWARE_POINTER_VELOCITY_X, velocityX)
                .put(HARDWARE_POINTER_VELOCITY_Y, velocityY)
                .build()
        }
    }
    class Scroll(motionEvent: MotionEvent, distanceX: Double, distanceY: Double): Gesture(motionEvent) {
        init {
            attributes = attributes.toBuilder()
                .put(HARDWARE_POINTER_DISTANCE_X, distanceX)
                .put(HARDWARE_POINTER_DISTANCE_Y, distanceY)
                .build()
        }
    }
}

private class TapEventMetadata(
    motionEvent: MotionEvent
) {

    val toolTypeDescription: String
    val buttonStateDescription: String?

    init {
        val toolTypeInt = motionEvent.getToolType(0)
        val toolTypeHasButtons =
            toolTypeInt == MotionEvent.TOOL_TYPE_MOUSE || toolTypeInt == MotionEvent.TOOL_TYPE_STYLUS
        val buttonStateInt = motionEvent.buttonState
        if (toolTypeHasButtons) {
            buttonStateDescription = buttonStateToString(buttonStateInt)
        } else {
            buttonStateDescription = null
        }
        toolTypeDescription = toolTypeToString(toolTypeInt)
    }

}


fun findTargetForTap(
    decorView: View,
    x: Float,
    y: Float,
    viewCoordinates: IntArray
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
            handleViewGroup(view, x, y, queue, viewCoordinates)
        }
    }
    return target
}


fun viewToName(view: View): String =
    try {
        view.resources?.getResourceEntryName(view.id) ?: view.id.toString()
    } catch (throwable: Throwable) {
        view.id.toString()
    }

private fun buttonStateToString(buttonStateInt: Int): String? {
    return when(buttonStateInt) {
        MotionEvent.BUTTON_PRIMARY, MotionEvent.BUTTON_STYLUS_PRIMARY -> "primary"
        MotionEvent.BUTTON_SECONDARY, MotionEvent.BUTTON_STYLUS_SECONDARY -> "secondary"
        MotionEvent.BUTTON_TERTIARY -> "tertiary"
        MotionEvent.BUTTON_BACK -> "back"
        MotionEvent.BUTTON_FORWARD -> "forward"
        else -> null
    }
}

private fun isJetpackComposeView(view: View): Boolean = view::class.java.name.startsWith("androidx.compose.ui.platform.ComposeView")

private fun handleViewGroup(
    view: ViewGroup,
    x: Float,
    y: Float,
    stack: LinkedList<View>,
    viewCoordinates: IntArray
) {
    if (!view.isVisible) return

    for (i in 0 until view.childCount) {
        val child = view.getChildAt(i)
        if (hitTest(child, x, y, viewCoordinates) && !isJetpackComposeView(child)) {
            stack.add(child)
        }
    }
}

private fun hitTest(
    view: View,
    x: Float,
    y: Float,
    viewCoordinates: IntArray
): Boolean {
    view.getLocationInWindow(viewCoordinates)
    val vx = viewCoordinates[0]
    val vy = viewCoordinates[1]

    val w = view.width
    val h = view.height
    return !(x < vx || x > vx + w || y < vy || y > vy + h)
}

private fun isValidClickTarget(view: View): Boolean = view.isClickable && view.isVisible

private val View.isVisible: Boolean
    get() = visibility == View.VISIBLE

