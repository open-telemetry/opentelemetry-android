/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common

import android.view.View
import android.view.ViewGroup

/**
 * Depth-first, iterative approach to recursively count all child views under the current View, and
 * compute the maximum depth.
 *
 * Returns a pair of [count, maxDepth]
 */
fun View.getComplexity(): Pair<Int, Int> {
    val stack = ArrayDeque<Pair<View, Int>>()
    var maxDepth = 0
    var count = 0

    stack.add(this to 0)

    while (stack.isNotEmpty()) {
        val (view, depth) = stack.removeFirst()
        maxDepth = maxOf(depth, maxDepth)
        count++

        if (view is ViewGroup) {
            stack.addAll(view.getChildren().map { it to (1 + depth) })
        }
    }

    return Pair(count, maxDepth)
}

fun ViewGroup.getChildren(): List<View> = (0 until this.childCount).map { this.getChildAt(it) }
