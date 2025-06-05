/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Owner
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import io.opentelemetry.instrumentation.compose.internal.APP_SCREEN_CLICK_EVENT_NAME
import io.opentelemetry.instrumentation.compose.internal.VIEW_CLICK_EVENT_NAME
import io.opentelemetry.instrumentation.compose.internal.viewIdAttr
import io.opentelemetry.instrumentation.compose.internal.viewNameAttr
import io.opentelemetry.instrumentation.compose.internal.xCoordinateAttr
import io.opentelemetry.instrumentation.compose.internal.yCoordinateAttr
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder
import io.opentelemetry.api.incubator.logs.ExtendedLogger
import java.lang.ref.WeakReference
import java.util.LinkedList

internal class ComposeClickEventGenerator(
    private val eventLogger: ExtendedLogger,
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
                    .setAttribute(yCoordinateAttr, motionEvent.y.toLong())
                    .setAttribute(xCoordinateAttr, motionEvent.x.toLong())
                    .emit()

                findTapTarget(window.decorView, motionEvent.x, motionEvent.y)
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

    private fun createEvent(name: String): ExtendedLogRecordBuilder =
        eventLogger
            .logRecordBuilder()
            .setEventName(name)

    private fun createNodeAttributes(node: LayoutNode): Attributes {
        val builder = Attributes.builder()
        builder.put(viewNameAttr, nodeToName(node))
        builder.put(viewIdAttr, node.semanticsId.toLong())

        getLayoutNodePositionInWindow(node)?.let {
            builder.put(xCoordinateAttr, it.x.toLong())
            builder.put(yCoordinateAttr, it.y.toLong())
        }
        return builder.build()
    }

    private fun nodeToName(node: LayoutNode): String =
        try {
            getNodeName(node) ?: node.semanticsId.toString()
        } catch (_: Throwable) {
            node.semanticsId.toString()
        }

    private fun findTapTarget(
        owner: Owner,
        x: Float,
        y: Float,
    ): LayoutNode? {
        val queue = LinkedList<LayoutNode>()
        queue.addFirst(owner.root)
        var target: LayoutNode? = null

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isPlaced && hitTest(node, x, y)) {
                target = node
            }

            queue.addAll(node.zSortedChildren.asMutableList())
        }
        return target
    }

    private fun findTapTarget(
        decorView: View,
        x: Float,
        y: Float,
    ) {
        val queue = LinkedList<View>()
        queue.addFirst(decorView)

        while (queue.isNotEmpty()) {
            val view = queue.removeFirst()
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    queue.add(view.getChildAt(index))
                }
                (view as? Owner)?.let {
                    try {
                        findTapTarget(
                            view as Owner,
                            x,
                            y
                        )?.let { layoutNode ->
                            createEvent(VIEW_CLICK_EVENT_NAME)
                                .setAllAttributes(createNodeAttributes(layoutNode))
                                .emit()
                        }
                    } catch (_: Throwable) {
                        // We rely on visibility suppression to access internal field,
                        // any runtime exception must be caught here.
                    }
                }
            }
        }
    }

    private fun isValidClickTarget(node: LayoutNode): Boolean {
        var isClickable = false

        for (info in node.getModifierInfo()) {
            val modifier = info.modifier
            if (modifier is SemanticsModifier) {
                with(modifier.semanticsConfiguration) {
                    if (contains(SemanticsActions.OnClick)) {
                        isClickable = true
                    }
                }
            } else {
                val className = modifier::class.qualifiedName
                if (className == CLASS_NAME_CLICKABLE_ELEMENT ||
                    className == CLASS_NAME_COMBINED_CLICKABLE_ELEMENT ||
                    className == CLASS_NAME_TOGGLEABLE_ELEMENT
                ) {
                    isClickable = true
                }
            }
        }

        return isClickable
    }

    private fun getNodeName(node: LayoutNode): String? {
        var className: String? = null
        for (info in node.getModifierInfo()) {
            val modifier = info.modifier
            if (modifier is SemanticsModifier) {
                with(modifier.semanticsConfiguration) {
                    val onClickSemanticsConfiguration = getOrNull(SemanticsActions.OnClick)
                    if (onClickSemanticsConfiguration != null) {
                        val accessibilityActionLabel = onClickSemanticsConfiguration.label
                        if (accessibilityActionLabel != null) {
                            return accessibilityActionLabel
                        }

                        val contentDescriptionSemanticsConfiguration =
                            getOrNull(SemanticsProperties.ContentDescription)
                        if (contentDescriptionSemanticsConfiguration != null) {
                            val contentDescription =
                                contentDescriptionSemanticsConfiguration.getOrNull(0)
                            if (contentDescription != null) {
                                return contentDescription
                            }
                        }
                    }
                }
            } else {
                className = modifier::class.qualifiedName
            }
        }

        return className
    }

    private fun hitTest(
        node: LayoutNode,
        x: Float,
        y: Float,
    ): Boolean {
        val bounded = getLayoutNodeBoundsInWindow(node)?.let { bounds ->
            x >= bounds.left && x <= bounds.right && y >= bounds.top && y <= bounds.bottom
        } == true

        return bounded && isValidClickTarget(node)
    }

    private fun getLayoutNodeBoundsInWindow(node: LayoutNode): Rect? {
        return try {
            node.layoutDelegate.outerCoordinator.coordinates.boundsInWindow()
        } catch (_: Exception) {
            null
        }
    }

    private fun getLayoutNodePositionInWindow(node: LayoutNode): Offset? {
        return try {
            node.layoutDelegate.outerCoordinator.coordinates.positionInWindow()
        } catch (_: Exception) {
            null
        }
    }


    companion object {
        private const val CLASS_NAME_CLICKABLE_ELEMENT =
            "androidx.compose.foundation.ClickableElement"
        private const val CLASS_NAME_COMBINED_CLICKABLE_ELEMENT =
            "androidx.compose.foundation.CombinedClickableElement"
        private const val CLASS_NAME_TOGGLEABLE_ELEMENT =
            "androidx.compose.foundation.selection.ToggleableElement"
    }
}
