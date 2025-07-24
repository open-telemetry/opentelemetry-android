/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose.click

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ComposeTapTargetDetectorTest {
    lateinit var composeTapTargetDetector: ComposeTapTargetDetector

    @MockK
    lateinit var composeLayoutNodeUtil: ComposeLayoutNodeUtil

    @MockK
    lateinit var composeView: AndroidComposeView

    @MockK
    lateinit var semanticsModifier: SemanticsModifier

    @MockK
    lateinit var modifier: Modifier

    @MockK
    lateinit var semanticsConfiguration: SemanticsConfiguration

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        composeTapTargetDetector = ComposeTapTargetDetector(composeLayoutNodeUtil)
    }

    @Test
    fun `name from onClick label`() {
        val name = composeTapTargetDetector.nodeToName(createMockLayoutNode(clickable = true))
        assertThat(name).isEqualTo("click")
    }

    @Test
    fun `name from description`() {
        val name =
            composeTapTargetDetector.nodeToName(
                createMockLayoutNode(
                    clickable = true,
                    useDescription = true,
                ),
            )
        assertThat(name).isEqualTo("clickMe")
    }

    @Test
    fun `name from id on exception`() {
        val mockNode = mockkClass(LayoutNode::class)
        every { mockNode.semanticsId } returns 41
        every { mockNode.getModifierInfo() } throws RuntimeException("test")

        val name =
            composeTapTargetDetector.nodeToName(
                mockNode,
            )
        assertThat(name).isEqualTo("41")
    }

    @Test
    fun `name from id on null`() {
        val mockNode = mockkClass(LayoutNode::class)
        every { mockNode.semanticsId } returns 41
        every { mockNode.getModifierInfo() } returns listOf()

        val name =
            composeTapTargetDetector.nodeToName(
                mockNode,
            )
        assertThat(name).isEqualTo("41")
    }

    @Test
    fun `return tap target when hit`() {
        val motionEvent =
            MotionEvent.obtain(0L, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 250f, 50f, 0)
        every { composeView.childCount } returns 0

        val mockLayoutNode =
            createMockLayoutNode(
                targetX = motionEvent.x,
                targetY = motionEvent.y,
                hit = true,
                clickable = true,
                useDescription = true,
            )
        every { composeView.root } returns mockLayoutNode

        val actual =
            composeTapTargetDetector.findTapTarget(composeView, motionEvent.x, motionEvent.y)
        assertThat(actual).isEqualTo(mockLayoutNode)
    }

    @Test
    fun `return null when no hit`() {
        val motionEvent =
            MotionEvent.obtain(0L, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 250f, 50f, 0)
        every { composeView.childCount } returns 0

        val mockLayoutNode =
            createMockLayoutNode(
                targetX = motionEvent.x,
                targetY = motionEvent.y,
                hit = false,
                clickable = true,
                useDescription = true,
            )
        every { composeView.root } returns mockLayoutNode

        val actual =
            composeTapTargetDetector.findTapTarget(composeView, motionEvent.x, motionEvent.y)
        assertThat(actual).isNull()
    }

    private fun createMockLayoutNode(
        targetX: Float = 0f,
        targetY: Float = 0f,
        hitOffset: IntArray = intArrayOf(10, 20),
        id: Int = 100,
        hit: Boolean = false,
        clickable: Boolean = false,
        useDescription: Boolean = false,
    ): LayoutNode {
        val mockNode = mockkClass(LayoutNode::class)
        every { mockNode.isPlaced } returns true

        val bounds =
            if (hit) {
                Rect(
                    left = targetX - hitOffset[0],
                    right = targetX + hitOffset[0],
                    top = targetY - hitOffset[1],
                    bottom = targetY + hitOffset[1],
                )
            } else {
                Rect(
                    left = targetX + hitOffset[0],
                    right = targetX + hitOffset[0],
                    top = targetY + hitOffset[1],
                    bottom = targetY + hitOffset[1],
                )
            }

        val mockModifierInfo = mockkClass(ModifierInfo::class)
        every { mockNode.getModifierInfo() } returns listOf(mockModifierInfo)
        if (clickable) {
            every { mockModifierInfo.modifier } returns semanticsModifier

            every { semanticsModifier.semanticsConfiguration } returns semanticsConfiguration
            every { semanticsConfiguration.contains(eq(SemanticsActions.OnClick)) } returns true

            if (useDescription) {
                every { semanticsConfiguration.getOrNull(eq(SemanticsActions.OnClick)) } returns null
                every { semanticsConfiguration.getOrNull(eq(SemanticsProperties.ContentDescription)) } returns
                    listOf(
                        "clickMe",
                    )
            } else {
                every { semanticsConfiguration.getOrNull(eq(SemanticsActions.OnClick)) } returns
                    AccessibilityAction<() -> Boolean>("click") { true }
            }

            every { mockNode.semanticsId } returns id
        } else {
            every { mockModifierInfo.modifier } returns modifier
        }

        every { mockNode.zSortedChildren } returns mutableVectorOf()
        every { composeLayoutNodeUtil.getLayoutNodeBoundsInWindow(mockNode) } returns bounds
        every { composeLayoutNodeUtil.getLayoutNodePositionInWindow(mockNode) } returns
            Offset(
                x = bounds.left,
                y = bounds.top,
            )

        return mockNode
    }
}
