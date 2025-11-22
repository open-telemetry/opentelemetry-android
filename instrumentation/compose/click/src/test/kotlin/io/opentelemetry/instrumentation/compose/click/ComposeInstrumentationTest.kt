/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose.click

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import android.view.Window
import android.view.Window.Callback
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeLayoutDelegate
import androidx.compose.ui.node.NodeCoordinator
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
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
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
internal class ComposeInstrumentationTest {
    private lateinit var openTelemetryRule: OpenTelemetryRule

    @MockK
    lateinit var window: Window

    @MockK
    lateinit var callback: Callback

    @MockK
    lateinit var activity: Activity

    @MockK
    lateinit var application: Application

    @MockK
    internal lateinit var composeView: AndroidComposeView

    @MockK
    lateinit var semanticsModifier: SemanticsModifier

    @MockK
    lateinit var modifier: Modifier

    @MockK
    lateinit var modifierInfo: ModifierInfo

    @MockK
    lateinit var semanticsConfiguration: SemanticsConfiguration

    @MockK
    lateinit var layoutDelegate: LayoutNodeLayoutDelegate

    @MockK
    lateinit var nodeCoordinator: NodeCoordinator

    @Before
    fun setup() {
        openTelemetryRule = OpenTelemetryRule.create()
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun capture_compose_click() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(relaxed = true),
            )

        val callbackCapturingSlot = slot<ComposeClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ComposeClickInstrumentation().install(installationContext)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val motionEvent =
            MotionEvent.obtain(0L, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 250f, 50f, 0)
        every { window.decorView } returns composeView
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

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(
            motionEvent,
        )

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        var event = events[0]
        assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfying(
                equalTo(APP_SCREEN_COORDINATE_X, motionEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, motionEvent.y.toLong()),
            )

        event = events[1]
        assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfying(
                equalTo(APP_WIDGET_ID, mockLayoutNode.semanticsId.toString()),
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

        every { mockNode.getModifierInfo() } returns listOf(modifierInfo)
        if (clickable) {
            every { modifierInfo.modifier } returns semanticsModifier

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
            every { modifierInfo.modifier } returns modifier
        }

        every { mockNode.zSortedChildren } returns mutableVectorOf()
        every { mockNode.layoutDelegate } returns layoutDelegate
        every { layoutDelegate.outerCoordinator } returns nodeCoordinator
        every { nodeCoordinator.coordinates } returns nodeCoordinator

        mockkStatic("androidx.compose.ui.layout.LayoutCoordinatesKt")
        every { nodeCoordinator.boundsInWindow() } returns bounds
        every { nodeCoordinator.positionInWindow() } returns Offset(x = bounds.left, y = bounds.top)

        return mockNode
    }
}
