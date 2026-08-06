/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.common

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_BUTTON_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_CLICKS_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_DISTANCE_X_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_DISTANCE_Y_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_TYPE_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_VELOCITY_X_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_VELOCITY_Y_KEY
import io.opentelemetry.api.common.Attributes
import java.util.LinkedList

internal fun toolTypeToString(toolTypeInt: Int): String =
    when (toolTypeInt) {
        MotionEvent.TOOL_TYPE_MOUSE -> "mouse"
        MotionEvent.TOOL_TYPE_FINGER -> "finger"
        MotionEvent.TOOL_TYPE_STYLUS -> "stylus"
        MotionEvent.TOOL_TYPE_ERASER -> "eraser"
        else -> "unknown"
    }

internal class TapEventMetadata(
    private val motionEvent: MotionEvent,
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

sealed class Gesture(
    val m: MotionEvent,
) {
    private val t = TapEventMetadata(m)
    val buttonStateDescription: String? = t.buttonStateDescription
    val toolTypeDescription: String = t.toolTypeDescription
    var attributes = Attributes.empty()

    init {
        attributes = attributes.toBuilder().put(HW_POINTER_TYPE_KEY, toolTypeDescription).build()
        if (buttonStateDescription != null) {
            attributes = attributes.toBuilder().put(HW_POINTER_BUTTON_KEY, buttonStateDescription).build()
        }
    }

    class LongPress(
        val motionEvent: MotionEvent,
    ) : Gesture(motionEvent)

    class Click(
        val motionEvent: MotionEvent,
        clicks: Int,
    ) : Gesture(motionEvent) {
        init {
            attributes = attributes.toBuilder().put(HW_POINTER_CLICKS_KEY, clicks.toLong()).build()
        }
    }

    class Fling(
        val motionEvent: MotionEvent,
        val velocityX: Double,
        val velocityY: Double,
    ) : Gesture(motionEvent) {
        init {
            attributes =
                attributes
                    .toBuilder()
                    .put(HW_POINTER_VELOCITY_X_KEY, velocityX)
                    .put(HW_POINTER_VELOCITY_Y_KEY, velocityY)
                    .build()
        }
    }

    class Scroll(
        val motionEvent: MotionEvent,
        val distanceX: Double,
        val distanceY: Double,
    ) : Gesture(motionEvent) {
        init {
            attributes =
                attributes
                    .toBuilder()
                    .put(HW_POINTER_DISTANCE_X_KEY, distanceX)
                    .put(HW_POINTER_DISTANCE_Y_KEY, distanceY)
                    .build()
        }
    }
}

fun findTargetForTap(
    decorView: View,
    x: Float,
    y: Float,
    viewCoordinates: IntArray,
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

private fun buttonStateToString(buttonStateInt: Int): String? =
    when (buttonStateInt) {
        MotionEvent.BUTTON_PRIMARY, MotionEvent.BUTTON_STYLUS_PRIMARY -> "primary"
        MotionEvent.BUTTON_SECONDARY, MotionEvent.BUTTON_STYLUS_SECONDARY -> "secondary"
        MotionEvent.BUTTON_TERTIARY -> "tertiary"
        MotionEvent.BUTTON_BACK -> "back"
        MotionEvent.BUTTON_FORWARD -> "forward"
        else -> null
    }

private fun isJetpackComposeView(view: View): Boolean = view::class.java.name.startsWith("androidx.compose.ui.platform.ComposeView")

private fun handleViewGroup(
    view: ViewGroup,
    x: Float,
    y: Float,
    stack: LinkedList<View>,
    viewCoordinates: IntArray,
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
    viewCoordinates: IntArray,
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
