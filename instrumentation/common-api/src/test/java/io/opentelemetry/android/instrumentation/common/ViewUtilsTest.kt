/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common

import android.view.View
import android.view.ViewGroup
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ViewUtilsTest {
    @Test
    fun `getComplexity returns 1 count and 0 depth for single view`() {
        val view = mockk<View>()

        val (count, depth) = view.getComplexity()

        assertEquals(1, count)
        assertEquals(0, depth)
    }

    @Test
    fun `getComplexity counts all children in ViewGroup`() {
        val parent = mockk<ViewGroup>()
        val child1 = mockk<View>()
        val child2 = mockk<View>()

        every { parent.childCount } returns 2
        every { parent.getChildAt(0) } returns child1
        every { parent.getChildAt(1) } returns child2

        val (count, depth) = parent.getComplexity()

        assertEquals(3, count)
        assertEquals(1, depth)
    }

    @Test
    fun `getComplexity calculates correct depth for nested ViewGroups`() {
        val root = mockk<ViewGroup>()
        val level1 = mockk<ViewGroup>()
        val level2 = mockk<View>()

        every { root.childCount } returns 1
        every { root.getChildAt(0) } returns level1
        every { level1.childCount } returns 1
        every { level1.getChildAt(0) } returns level2

        val (count, depth) = root.getComplexity()

        assertEquals(3, count)
        assertEquals(2, depth)
    }

    @Test
    fun `getComplexity handles complex tree structure`() {
        val root = mockk<ViewGroup>()
        val child1 = mockk<ViewGroup>()
        val child2 = mockk<View>()
        val grandchild1 = mockk<View>()
        val grandchild2 = mockk<View>()

        every { root.childCount } returns 2
        every { root.getChildAt(0) } returns child1
        every { root.getChildAt(1) } returns child2
        every { child1.childCount } returns 2
        every { child1.getChildAt(0) } returns grandchild1
        every { child1.getChildAt(1) } returns grandchild2

        val (count, depth) = root.getComplexity()

        assertEquals(5, count)
        assertEquals(2, depth)
    }
}
