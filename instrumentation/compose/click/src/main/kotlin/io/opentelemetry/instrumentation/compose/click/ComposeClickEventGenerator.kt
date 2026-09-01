/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose.click

import android.view.MotionEvent
import android.view.Window
import androidx.compose.ui.node.LayoutNode
import io.opentelemetry.android.semconv.events.AppScreenClickEvent
import io.opentelemetry.android.semconv.events.AppWidgetClickEvent
import io.opentelemetry.api.logs.Logger
import java.lang.ref.WeakReference
import kotlin.let

internal class ComposeClickEventGenerator(
    private val eventLogger: Logger,
    private val composeLayoutNodeUtil: ComposeLayoutNodeUtil = ComposeLayoutNodeUtil(),
    private val composeTapTargetDetector: ComposeTapTargetDetector = ComposeTapTargetDetector(composeLayoutNodeUtil),
    private val recordActivity: () -> Unit = {},
) {
    private var windowRef: WeakReference<Window>? = null

    fun startTracking(window: Window) {
        windowRef = WeakReference(window)
        val currentCallback = window.callback
        window.callback = WindowCallbackWrapper(currentCallback, this)
    }

    fun generateClick(motionEvent: MotionEvent?) {
        windowRef?.get()?.let { window ->
            if (motionEvent?.actionMasked == MotionEvent.ACTION_DOWN) {
                recordActivity()
            } else if (motionEvent != null && motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                AppScreenClickEvent(
                    appScreenCoordinateX = motionEvent.x.toLong(),
                    appScreenCoordinateY = motionEvent.y.toLong(),
                ).emit(eventLogger)

                val node: LayoutNode? = composeTapTargetDetector.findTapTarget(window.decorView, motionEvent.x, motionEvent.y)
                node?.let { layoutNode ->
                    val position = composeLayoutNodeUtil.getLayoutNodePositionInWindow(layoutNode)
                    AppWidgetClickEvent(
                        appScreenCoordinateX = position?.x?.toLong(),
                        appScreenCoordinateY = position?.y?.toLong(),
                        appWidgetId = layoutNode.semanticsId.toString(),
                        appWidgetName = composeTapTargetDetector.nodeToName(layoutNode),
                    ).emit(eventLogger)
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
}
