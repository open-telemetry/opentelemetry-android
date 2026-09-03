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
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.semconv.AppAttributes.APP_SCREEN_COORDINATE_X_KEY
import io.opentelemetry.android.semconv.AppAttributes.APP_SCREEN_COORDINATE_Y_KEY
import io.opentelemetry.android.semconv.AppAttributes.APP_WIDGET_ID_KEY
import io.opentelemetry.android.semconv.AppAttributes.APP_WIDGET_NAME_KEY
import io.opentelemetry.android.semconv.events.AppScreenClickEvent.Companion.APP_SCREEN_CLICK_EVENT_NAME
import io.opentelemetry.android.semconv.events.AppWidgetClickEvent.Companion.APP_WIDGET_CLICK_EVENT_NAME
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
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
    fun `activity failure does not block touch dispatch`() {
        val recordActivity = mockk<() -> Unit>()
        every { recordActivity() } throws IllegalStateException("activity failed")
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns true

        val generator =
            ComposeClickEventGenerator(
                eventLogger = mockk(relaxed = true),
                recordActivity = recordActivity,
            )
        val wrapper = slot<WindowCallbackWrapper>()
        every { window.callback = capture(wrapper) } returns Unit
        generator.startTracking(window)

        val event = MotionEvent.obtain(0L, SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        try {
            wrapper.captured.dispatchTouchEvent(event)

            verify(exactly = 1) { recordActivity() }
            verify(exactly = 1) { callback.dispatchTouchEvent(event) }
        } finally {
            event.recycle()
        }
    }

    @Test
    fun capture_compose_click() {
        val activitySessionProvider = mockk<SessionProvider>(relaxUnitFun = true)
        every { activitySessionProvider.recordActivity() } answers {
            assertThat(openTelemetryRule.logRecords).isEmpty()
        }
        val openTelemetryRum =
            mockk<OpenTelemetryRum> {
                every { openTelemetry } returns openTelemetryRule.openTelemetry
                every { sessionProvider } returns activitySessionProvider
                every { clock } returns Clock.getDefault()
            }

        val callbackCapturingSlot = slot<ComposeClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ComposeClickInstrumentation().install(application, openTelemetryRum)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        val motionEvent = createMotionEvent(MotionEvent.ACTION_UP)
        every { window.decorView } returns composeView
        every { composeView.childCount } returns 0

        val mockLayoutNode: LayoutNode =
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

        try {
            listOf(downEvent, motionEvent).forEach { wrapperCapturingSlot.captured.dispatchTouchEvent(it) }
            verify(exactly = 1) { activitySessionProvider.recordActivity() }
            assertComposeClickEvents(motionEvent, mockLayoutNode)
        } finally {
            downEvent.recycle()
            motionEvent.recycle()
        }
    }

    private fun assertComposeClickEvents(
        motionEvent: MotionEvent,
        mockLayoutNode: LayoutNode,
    ) {
        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        assertThat(events[0])
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X_KEY, motionEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y_KEY, motionEvent.y.toLong()),
            )

        assertThat(events[1])
            .hasEventName(APP_WIDGET_CLICK_EVENT_NAME)
            .hasAttributesSatisfying(
                equalTo(APP_WIDGET_ID_KEY, mockLayoutNode.semanticsId.toString()),
                equalTo(APP_WIDGET_NAME_KEY, "clickMe"),
            )
    }

    private fun createMotionEvent(action: Int): MotionEvent = MotionEvent.obtain(0L, SystemClock.uptimeMillis(), action, 250f, 50f, 0)

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
