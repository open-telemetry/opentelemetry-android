/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose.click

import android.os.SystemClock
import android.view.MotionEvent
import android.view.Window
import android.view.Window.Callback
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
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_SCREEN_COORDINATE_X
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_SCREEN_COORDINATE_Y
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_WIDGET_ID
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_WIDGET_NAME
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ComposeClickEventGeneratorTest {
    private lateinit var openTelemetryRule: OpenTelemetryRule

    private lateinit var composeClickEventGenerator: ComposeClickEventGenerator

    @MockK
    lateinit var composeLayoutNodeUtil: ComposeLayoutNodeUtil

    @MockK
    lateinit var window: Window

    @MockK
    lateinit var callback: Callback

    @MockK
    internal lateinit var composeView: AndroidComposeView

    @MockK
    lateinit var semanticsModifier: SemanticsModifier

    @MockK
    lateinit var modifier: Modifier

    @MockK
    lateinit var semanticsConfiguration: SemanticsConfiguration

    @Before
    fun setup() {
        openTelemetryRule = OpenTelemetryRule.create()
        MockKAnnotations.init(this, relaxUnitFun = true)
        composeClickEventGenerator =
            ComposeClickEventGenerator(
                openTelemetryRule.openTelemetry.logsBridge
                    .loggerBuilder("io.opentelemetry.android.instrumentation.compose")
                    .build(),
                SessionProvider.getNoop(),
                composeLayoutNodeUtil,
            )

        every { window.callback } returns callback
        every { window.decorView } returns composeView
        every { window.callback = any() } returns Unit

        composeClickEventGenerator.startTracking(window)
    }

    @Test
    fun `capture click for a single hit target`() {
        val motionEvent =
            MotionEvent.obtain(0L, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 250f, 50f, 0)

        every { composeView.childCount } returns 0

        buildMockLayoutNodeTree(
            targetX = motionEvent.x,
            targetY = motionEvent.y,
            hitIndexes = listOf(2),
            clickableIndexes = listOf(2, 3, 4),
        )

        composeClickEventGenerator.generateClick(motionEvent)

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        var event = events[0]
        OpenTelemetryAssertions
            .assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfying(
                equalTo(APP_SCREEN_COORDINATE_X, motionEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, motionEvent.y.toLong()),
            )

        event = events[1]
        assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfying(
                equalTo(APP_WIDGET_ID, "2"),
                equalTo(APP_WIDGET_NAME, "click"),
            )
    }

    @Test
    fun `capture click when there are two valid targets but the top target wins`() {
        val motionEvent =
            MotionEvent.obtain(0L, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 250f, 50f, 0)

        every { composeView.childCount } returns 0

        buildMockLayoutNodeTree(
            targetX = motionEvent.x,
            targetY = motionEvent.y,
            hitIndexes = listOf(3, 4),
            clickableIndexes = listOf(2, 3, 4),
        )

        composeClickEventGenerator.generateClick(motionEvent)

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        var event = events[0]
        OpenTelemetryAssertions
            .assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfying(
                equalTo(APP_SCREEN_COORDINATE_X, motionEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, motionEvent.y.toLong()),
            )

        event = events[1]
        assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfying(
                equalTo(APP_WIDGET_ID, "3"),
                equalTo(APP_WIDGET_NAME, "click"),
            )
    }

    @Test
    fun `capture click when there are two valid targets but the top target wins and use content description for name`() {
        val motionEvent =
            MotionEvent.obtain(0L, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 250f, 50f, 0)

        every { composeView.childCount } returns 0

        buildMockLayoutNodeTree(
            targetX = motionEvent.x,
            targetY = motionEvent.y,
            hitIndexes = listOf(3, 4),
            clickableIndexes = listOf(2, 3, 4),
            describableIndexes = listOf(3, 4),
        )

        composeClickEventGenerator.generateClick(motionEvent)

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        var event = events[0]
        OpenTelemetryAssertions
            .assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfying(
                equalTo(APP_SCREEN_COORDINATE_X, motionEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, motionEvent.y.toLong()),
            )

        event = events[1]
        assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfying(
                equalTo(APP_WIDGET_ID, "3"),
                equalTo(APP_WIDGET_NAME, "clickMe"),
            )
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

        every { composeLayoutNodeUtil.getLayoutNodeBoundsInWindow(mockNode) } returns bounds
        every { composeLayoutNodeUtil.getLayoutNodePositionInWindow(mockNode) } returns
            Offset(
                x = bounds.left,
                y = bounds.top,
            )

        return mockNode
    }

    private fun buildMockLayoutNodeTree(
        targetX: Float,
        targetY: Float,
        hitIndexes: List<Int> = emptyList(),
        clickableIndexes: List<Int> = emptyList(),
        describableIndexes: List<Int> = emptyList(),
    ) {
        val nodeList = mutableListOf<LayoutNode>()
        for (i in 0 until 5) {
            nodeList.add(
                createMockLayoutNode(
                    targetX = targetX,
                    targetY = targetY,
                    id = i,
                    hit = hitIndexes.contains(i),
                    clickable = clickableIndexes.contains(i),
                    useDescription = describableIndexes.contains(i),
                ),
            )
        }

        every { nodeList[0].zSortedChildren } returns mutableVectorOf(nodeList[1], nodeList[2])
        every { nodeList[1].zSortedChildren } returns mutableVectorOf(nodeList[4], nodeList[3])
        every { nodeList[2].zSortedChildren } returns mutableVectorOf()

        every { nodeList[3].zSortedChildren } returns mutableVectorOf()
        every { nodeList[4].zSortedChildren } returns mutableVectorOf()
        every { composeView.root } returns nodeList[0]
    }
}
