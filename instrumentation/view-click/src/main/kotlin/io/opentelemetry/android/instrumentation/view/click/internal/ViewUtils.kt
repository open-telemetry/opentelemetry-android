/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click.internal

import android.view.MotionEvent
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_BUTTON_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_CLICKS_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_DISTANCE_X_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_DISTANCE_Y_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_TYPE_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_VELOCITY_X_KEY
import io.opentelemetry.android.semconv.HwAttributes.HW_POINTER_VELOCITY_Y_KEY
import io.opentelemetry.api.common.Attributes

internal const val APP_SCREEN_CLICK_EVENT_NAME = "app.screen.click"
internal const val VIEW_CLICK_EVENT_NAME = "app.widget.click"

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
