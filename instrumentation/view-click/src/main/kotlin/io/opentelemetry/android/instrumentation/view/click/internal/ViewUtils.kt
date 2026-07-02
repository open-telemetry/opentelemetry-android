/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click.internal

import android.view.MotionEvent
import io.opentelemetry.api.common.Attributes

internal const val APP_SCREEN_CLICK_EVENT_NAME = "app.screen.click"
internal const val VIEW_CLICK_EVENT_NAME = "app.widget.click"
internal const val APP_SCREEN_LONG_PRESS_EVENT_NAME = "app.screen.longpress"
internal const val VIEW_LONG_PRESS_EVENT_NAME = "app.widget.longpress"

internal const val HARDWARE_POINTER_TYPE = "hw.pointer.type"

internal const val HARDWARE_POINTER_BUTTON = "hw.pointer.button"

internal const val HARDWARE_POINTER_CLICKS = "hw.pointer.clicks"

internal const val APP_SCREEN_SCROLL_EVENT_NAME = "app.screen.scroll"

internal const val VIEW_SCROLL_EVENT_NAME = "app.widget.scroll"

internal const val APP_SCREEN_FLING_EVENT_NAME = "app.screen.fling"

internal const val VIEW_FLING_EVENT_NAME = "app.widget.fling"

internal const val HARDWARE_POINTER_VELOCITY_X = "hw.pointer.velocity.x"

internal const val HARDWARE_POINTER_VELOCITY_Y = "hw.pointer.velocity.y"

internal const val HARDWARE_POINTER_DISTANCE_X = "hw.pointer.distance.x"

internal const val HARDWARE_POINTER_DISTANCE_Y = "hw.pointer.distance.y"

internal fun buttonStateToString(buttonStateInt: Int): String? =
    when (buttonStateInt) {
        MotionEvent.BUTTON_PRIMARY, MotionEvent.BUTTON_STYLUS_PRIMARY -> "primary"
        MotionEvent.BUTTON_SECONDARY, MotionEvent.BUTTON_STYLUS_SECONDARY -> "secondary"
        MotionEvent.BUTTON_TERTIARY -> "tertiary"
        MotionEvent.BUTTON_BACK -> "back"
        MotionEvent.BUTTON_FORWARD -> "forward"
        else -> null
    }

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

internal sealed class Gesture(
    val m: MotionEvent,
) {
    private val t = TapEventMetadata(m)
    var attributes = Attributes.empty()

    init {
        attributes = attributes.toBuilder().put(HARDWARE_POINTER_TYPE, t.toolTypeDescription).build()
        if (t.buttonStateDescription != null) {
            attributes = attributes.toBuilder().put(HARDWARE_POINTER_BUTTON, t.buttonStateDescription).build()
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
            attributes = attributes.toBuilder().put(HARDWARE_POINTER_CLICKS, clicks.toLong()).build()
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
                    .put(HARDWARE_POINTER_VELOCITY_X, velocityX)
                    .put(HARDWARE_POINTER_VELOCITY_Y, velocityY)
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
                    .put(HARDWARE_POINTER_DISTANCE_X, distanceX)
                    .put(HARDWARE_POINTER_DISTANCE_Y, distanceY)
                    .build()
        }
    }
}
