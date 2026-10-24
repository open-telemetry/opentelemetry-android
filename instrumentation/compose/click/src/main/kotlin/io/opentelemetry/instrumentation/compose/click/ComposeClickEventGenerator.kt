/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose.click

import android.view.MotionEvent
import android.view.Window
import androidx.compose.ui.node.LayoutNode
import io.opentelemetry.android.instrumentation.common.ToggleableTracker
import io.opentelemetry.android.instrumentation.common.TouchEventConsumer
import io.opentelemetry.android.instrumentation.common.WindowCallbackWrapper
import io.opentelemetry.kotlin.logging.Logger
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.events.AppScreenClickEvent
import io.opentelemetry.kotlin.semconv.events.AppWidgetClickEvent
import java.lang.ref.WeakReference
import kotlin.let

@OptIn(IncubatingApi::class)
internal class ComposeClickEventGenerator(
    private val eventLogger: Logger,
    private val composeLayoutNodeUtil: ComposeLayoutNodeUtil = ComposeLayoutNodeUtil(),
    private val composeTapTargetDetector: ComposeTapTargetDetector = ComposeTapTargetDetector(composeLayoutNodeUtil),
): TouchEventConsumer, ToggleableTracker {
    private var windowRef: WeakReference<Window>? = null

    override fun startTracking(window: Window) {
        windowRef = WeakReference(window)
        WindowCallbackWrapper.registerConsumer(window, WRAPPER_KEY_COMPOSE_CLICK, this)
    }

    override fun consumeEvent(motionEvent: MotionEvent?) {
        windowRef?.get()?.let { window ->
            if (motionEvent != null && motionEvent.actionMasked == MotionEvent.ACTION_UP) {
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

    override fun stopTracking() {
        windowRef?.get()?.run {
            WindowCallbackWrapper.unregisterConsumer(this, WRAPPER_KEY_COMPOSE_CLICK)
        }
        windowRef = null
    }

    companion object {
        private const val WRAPPER_KEY_COMPOSE_CLICK = "compose-click-wrapper"
    }
}
