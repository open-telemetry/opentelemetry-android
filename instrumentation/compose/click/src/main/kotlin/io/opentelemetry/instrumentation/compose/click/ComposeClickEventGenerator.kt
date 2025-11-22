/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose.click

import android.view.MotionEvent
import android.view.Window
import androidx.compose.ui.node.LayoutNode
import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.android.ktx.setSessionIdentifiersWith
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_SCREEN_COORDINATE_X
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_SCREEN_COORDINATE_Y
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_WIDGET_ID
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_WIDGET_NAME
import java.lang.ref.WeakReference
import kotlin.let

@OptIn(Incubating::class)
internal class ComposeClickEventGenerator(
    private val eventLogger: Logger,
    private val sessionProvider: SessionProvider,
    private val composeLayoutNodeUtil: ComposeLayoutNodeUtil = ComposeLayoutNodeUtil(),
    private val composeTapTargetDetector: ComposeTapTargetDetector = ComposeTapTargetDetector(composeLayoutNodeUtil),
) {
    private var windowRef: WeakReference<Window>? = null

    fun startTracking(window: Window) {
        windowRef = WeakReference(window)
        val currentCallback = window.callback
        window.callback = WindowCallbackWrapper(currentCallback, this)
    }

    fun generateClick(motionEvent: MotionEvent?) {
        windowRef?.get()?.let { window ->
            if (motionEvent != null && motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                createEvent(APP_SCREEN_CLICK_EVENT_NAME)
                    .setAttribute(APP_SCREEN_COORDINATE_Y, motionEvent.y.toLong())
                    .setAttribute(APP_SCREEN_COORDINATE_X, motionEvent.x.toLong())
                    .emit()

                composeTapTargetDetector.findTapTarget(window.decorView, motionEvent.x, motionEvent.y)?.let { layoutNode ->
                    createEvent(VIEW_CLICK_EVENT_NAME)
                        .setAllAttributes(createNodeAttributes(layoutNode))
                        .emit()
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

    private fun createEvent(name: String): LogRecordBuilder =
        eventLogger
            .logRecordBuilder()
            .setSessionIdentifiersWith(sessionProvider)
            .setEventName(name)

    private fun createNodeAttributes(node: LayoutNode): Attributes {
        val builder = Attributes.builder()
        builder.put(APP_WIDGET_NAME, composeTapTargetDetector.nodeToName(node))
        builder.put(APP_WIDGET_ID, node.semanticsId.toString())

        composeLayoutNodeUtil.getLayoutNodePositionInWindow(node)?.let {
            builder.put(APP_SCREEN_COORDINATE_X, it.x.toLong())
            builder.put(APP_SCREEN_COORDINATE_Y, it.y.toLong())
        }
        return builder.build()
    }
}
