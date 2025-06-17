/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose.click

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Owner
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import java.util.LinkedList

internal class ComposeTapTargetDetector(
    private val composeLayoutNodeUtil: ComposeLayoutNodeUtil,
) {
    fun nodeToName(node: LayoutNode): String =
        try {
            getNodeName(node) ?: node.semanticsId.toString()
        } catch (_: Throwable) {
            node.semanticsId.toString()
        }

    fun findTapTarget(
        decorView: View,
        x: Float,
        y: Float,
    ): LayoutNode? {
        val queue = LinkedList<View>()
        queue.addFirst(decorView)

        var target: LayoutNode? = null
        while (queue.isNotEmpty()) {
            val view = queue.removeFirst()
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    queue.add(view.getChildAt(index))
                }
                (view as? Owner)?.let {
                    try {
                        target =
                            findTapTarget(
                                view as Owner,
                                x,
                                y,
                            )
                    } catch (_: Throwable) {
                        // We rely on visibility suppression to access internal fields and
                        // classes any runtime exception must be caught here.
                    }
                }
            }
        }
        return target
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

    private fun isValidClickTarget(node: LayoutNode): Boolean {
        for (info in node.getModifierInfo()) {
            val modifier = info.modifier
            if (modifier is SemanticsModifier) {
                with(modifier.semanticsConfiguration) {
                    if (contains(SemanticsActions.OnClick)) {
                        return true
                    }
                }
            } else {
                val className = modifier::class.qualifiedName
                if (
                    className == CLASS_NAME_CLICKABLE_ELEMENT ||
                    className == CLASS_NAME_COMBINED_CLICKABLE_ELEMENT ||
                    className == CLASS_NAME_TOGGLEABLE_ELEMENT
                ) {
                    return true
                }
            }
        }

        return false
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
        val bounded =
            composeLayoutNodeUtil.getLayoutNodeBoundsInWindow(node)?.let { bounds ->
                x >= bounds.left && x <= bounds.right && y >= bounds.top && y <= bounds.bottom
            } == true

        return bounded && isValidClickTarget(node)
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
