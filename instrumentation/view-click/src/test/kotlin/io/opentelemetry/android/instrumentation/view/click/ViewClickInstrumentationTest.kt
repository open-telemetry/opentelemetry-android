/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.Window.Callback
import androidx.test.ext.junit.runners.AndroidJUnit4
import fastForwardDoubleTapTimeout
import getDoubleTapSequence
import getSingleTapSequence
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.view.click.internal.APP_SCREEN_CLICK_EVENT_NAME
import io.opentelemetry.android.instrumentation.view.click.internal.HARDWARE_POINTER_BUTTON
import io.opentelemetry.android.instrumentation.view.click.internal.HARDWARE_POINTER_CLICKS
import io.opentelemetry.android.instrumentation.view.click.internal.HARDWARE_POINTER_TYPE
import io.opentelemetry.android.instrumentation.view.click.internal.VIEW_CLICK_EVENT_NAME
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_SCREEN_COORDINATE_X
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_SCREEN_COORDINATE_Y
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_WIDGET_ID
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_WIDGET_NAME
import mockView
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExtendWith(MockKExtension::class)
class ViewClickInstrumentationTest {
    private lateinit var openTelemetryRule: OpenTelemetryRule

    @MockK
    lateinit var window: Window

    @MockK
    lateinit var callback: Callback

    @MockK
    lateinit var activity: Activity

    @MockK
    lateinit var application: Application

    @Before
    fun setUp() {
        openTelemetryRule = OpenTelemetryRule.create()
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    private val clicksKey = longKey(HARDWARE_POINTER_CLICKS)
    private val toolTypeKey = stringKey(HARDWARE_POINTER_TYPE)
    private val buttonKey = stringKey(HARDWARE_POINTER_BUTTON)

    @Test
    fun capture_view_click() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<ViewClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewClickInstrumentation().install(installationContext)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val singleTapSequence = getSingleTapSequence(250f, 50f)
        val motionEvent = singleTapSequence[0]

        val mockView = mockView<View>(10012, motionEvent)
        every { window.decorView } returns mockView

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(
            singleTapSequence[0],
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            singleTapSequence[1],
        )
        fastForwardDoubleTapTimeout()

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        var event = events[0]
        assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, motionEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, motionEvent.y.toLong()),
                equalTo(clicksKey, 1.toLong()),
                equalTo(toolTypeKey, "finger")
            )

        event = events[1]
        assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, mockView.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, mockView.y.toLong()),
                equalTo(APP_WIDGET_ID, mockView.id.toString()),
                equalTo(APP_WIDGET_NAME, "10012"),
                equalTo(clicksKey, 1.toLong()),
                equalTo(toolTypeKey, "finger")
            )
    }

    @Test
    fun capture_view_click_in_viewGroup() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<ViewClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewClickInstrumentation().install(installationContext)
        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val singleTapSequence = getSingleTapSequence(250f, 50f)
        val motionEvent = singleTapSequence[0]
        val mockView = mockView<View>(10012, motionEvent)
        val mockViewGroup =
            mockView<ViewGroup>(10013, motionEvent, clickable = false) {
                every { it.childCount } returns 1
                every { it.getChildAt(any()) } returns mockView
            }

        every { window.decorView } returns mockViewGroup
        viewClickActivityCallback.onActivityResumed(activity)

        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(singleTapSequence[0])

        wrapperCapturingSlot.captured.dispatchTouchEvent(singleTapSequence[1])
        fastForwardDoubleTapTimeout()

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        var event = events[0]
        assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, motionEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, motionEvent.y.toLong()),
                equalTo(clicksKey, 1.toLong()),
                equalTo(toolTypeKey, "finger")
            )

        event = events[1]
        assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, mockView.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, mockView.y.toLong()),
                equalTo(APP_WIDGET_ID, mockView.id.toString()),
                equalTo(APP_WIDGET_NAME, "10012"),
                equalTo(clicksKey, 1.toLong()),
                equalTo(toolTypeKey, "finger")
            )
    }

    @Test
    fun not_captured_view_click_in_viewGroup() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<ViewClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewClickInstrumentation().install(installationContext)
        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val singleTapSequence = getSingleTapSequence(250f, 50f)
        val motionEvent = singleTapSequence[0]
        val mockView = mockView<View>(10012, motionEvent, hitOffset = intArrayOf(50, 30))
        val mockViewGroup =
            mockView<ViewGroup>(10013, motionEvent, clickable = false) {
                every { it.childCount } returns 1
                every { it.getChildAt(any()) } returns mockView
            }

        every { window.decorView } returns mockViewGroup

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(
            singleTapSequence[0],
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            singleTapSequence[1],
        )
        fastForwardDoubleTapTimeout()

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(1)

        val event = events[0]
        assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, motionEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, motionEvent.y.toLong()),
                equalTo(clicksKey, 1.toLong()),
                equalTo(toolTypeKey, "finger")
            )
    }

    @Test
    fun not_captured_view_click_for_down_event() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<ViewClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewClickInstrumentation().install(installationContext)
        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val motionEvent =
            MotionEvent.obtain(0L, SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 250f, 50f, 0)

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(
            motionEvent,
        )

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(0)
    }


    @Test
    fun capture_view_double_tap() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<ViewClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { window.context } returns application
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewClickInstrumentation().install(installationContext)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val doubleTapSequence = getDoubleTapSequence(250f, 50f)
        val initialDownEvent = doubleTapSequence[0]

        val mockView = mockView<View>(10012, initialDownEvent)
        every { window.decorView } returns mockView

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        for(motionEvent in doubleTapSequence) {
            wrapperCapturingSlot.captured.dispatchTouchEvent(motionEvent)
        }
        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        var event = events[0]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, initialDownEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, initialDownEvent.y.toLong()),
                equalTo(clicksKey, 2.toLong()),
                equalTo(toolTypeKey, "finger")
            )

        event = events[1]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, mockView.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, mockView.y.toLong()),
                equalTo(APP_WIDGET_ID, mockView.id.toString()),
                equalTo(APP_WIDGET_NAME, "10012"),
                equalTo(clicksKey, 2.toLong()),
                equalTo(toolTypeKey, "finger")
            )
    }

    @Test
    fun capture_view_double_tap_button_state() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<ViewClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { window.context } returns application
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewClickInstrumentation().install(installationContext)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val doubleTapSequence = getDoubleTapSequence(250f, 50f, MotionEvent.TOOL_TYPE_MOUSE, MotionEvent.BUTTON_PRIMARY)
        val initialDownEvent = doubleTapSequence[0]

        val mockView = mockView<View>(10012, initialDownEvent)
        every { window.decorView } returns mockView

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        for(motionEvent in doubleTapSequence) {
            wrapperCapturingSlot.captured.dispatchTouchEvent(motionEvent)
        }
        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        var event = events[0]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, initialDownEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, initialDownEvent.y.toLong()),
                equalTo(clicksKey, 2.toLong()),
                equalTo(toolTypeKey, "mouse"),
                equalTo(buttonKey, "primary")
            )

        event = events[1]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, mockView.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, mockView.y.toLong()),
                equalTo(APP_WIDGET_ID, mockView.id.toString()),
                equalTo(APP_WIDGET_NAME, "10012"),
                equalTo(clicksKey, 2.toLong()),
                equalTo(toolTypeKey, "mouse"),
                equalTo(buttonKey, "primary")
            )
    }

    @Test
    fun capture_view_single_tap_button_state() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<ViewClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewClickInstrumentation().install(installationContext)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val singleTapSequence = getSingleTapSequence(250f, 50f, MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.BUTTON_STYLUS_SECONDARY)
        val motionEvent = singleTapSequence[0]

        val mockView = mockView<View>(10012, motionEvent)
        every { window.decorView } returns mockView

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(singleTapSequence[0])
        wrapperCapturingSlot.captured.dispatchTouchEvent(singleTapSequence[1])
        fastForwardDoubleTapTimeout()

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        var event = events[0]
        assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, motionEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, motionEvent.y.toLong()),
                equalTo(clicksKey, 1.toLong()),
                equalTo(toolTypeKey, "stylus"),
                equalTo(buttonKey, "secondary")
            )

        event = events[1]
        assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, mockView.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, mockView.y.toLong()),
                equalTo(APP_WIDGET_ID, mockView.id.toString()),
                equalTo(APP_WIDGET_NAME, "10012"),
                equalTo(clicksKey, 1.toLong()),
                equalTo(toolTypeKey, "stylus"),
                equalTo(buttonKey, "secondary")
            )
    }

    @Test
    fun capture_view_single_tap_when_double_tap_timeout_exceeded() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<ViewClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { window.context } returns application
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewClickInstrumentation().install(installationContext)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit


        val doubleTapSequence = getDoubleTapSequence(250f, 50f, exceedTimeOut = true)
        val initialDownEvent = doubleTapSequence[0]

        val mockView = mockView<View>(10012, initialDownEvent)
        every { window.decorView } returns mockView

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(doubleTapSequence[0])
        wrapperCapturingSlot.captured.dispatchTouchEvent(doubleTapSequence[1])
        fastForwardDoubleTapTimeout()
        wrapperCapturingSlot.captured.dispatchTouchEvent(doubleTapSequence[2])
        wrapperCapturingSlot.captured.dispatchTouchEvent(doubleTapSequence[3])

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)


        var event = events[0]
        assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, initialDownEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, initialDownEvent.y.toLong()),
                equalTo(clicksKey, 1.toLong()),
                equalTo(toolTypeKey, "finger")
            )

        event = events[1]
        assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, mockView.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, mockView.y.toLong()),
                equalTo(APP_WIDGET_ID, mockView.id.toString()),
                equalTo(APP_WIDGET_NAME, "10012"),
                equalTo(clicksKey, 1.toLong()),
                equalTo(toolTypeKey, "finger")
            )
    }

    @Test
    fun not_captured_view_double_tap_in_viewGroup() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<ViewClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewClickInstrumentation().install(installationContext)
        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val doubleTapSequence = getDoubleTapSequence(250f, 50f)
        val initialDownEvent = doubleTapSequence[0]

        val mockView = mockView<View>(10012, initialDownEvent, hitOffset = intArrayOf(50, 30))
        val mockViewGroup =
            mockView<ViewGroup>(10013, initialDownEvent, clickable = false) {
                every { it.childCount } returns 1
                every { it.getChildAt(any()) } returns mockView
            }

        every { window.decorView } returns mockViewGroup

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        for(motionEvent in doubleTapSequence) {
            wrapperCapturingSlot.captured.dispatchTouchEvent(motionEvent)
        }

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(1)

        val event = events[0]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, initialDownEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, initialDownEvent.y.toLong()),
                equalTo(clicksKey, 2.toLong()),
                equalTo(toolTypeKey, "finger")
            )
    }

    @Test
    fun capture_view_double_tap_in_viewGroup() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<ViewClickActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewClickInstrumentation().install(installationContext)
        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val doubleTapSequence = getDoubleTapSequence(250f, 50f)
        val initialDownEvent = doubleTapSequence[0]

        val mockView = mockView<View>(10012, initialDownEvent)
        val mockViewGroup =
            mockView<ViewGroup>(10013, initialDownEvent, clickable = false) {
                every { it.childCount } returns 1
                every { it.getChildAt(any()) } returns mockView
            }

        every { window.decorView } returns mockViewGroup
        viewClickActivityCallback.onActivityResumed(activity)

        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        for (motionEvent in doubleTapSequence) {
            wrapperCapturingSlot.captured.dispatchTouchEvent(motionEvent)
        }

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)

        var event = events[0]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(APP_SCREEN_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, initialDownEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, initialDownEvent.y.toLong()),
                equalTo(clicksKey, 2.toLong()),
                equalTo(toolTypeKey, "finger")
            )

        event = events[1]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(VIEW_CLICK_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, mockView.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, mockView.y.toLong()),
                equalTo(APP_WIDGET_ID, mockView.id.toString()),
                equalTo(APP_WIDGET_NAME, "10012"),
                equalTo(clicksKey, 2.toLong()),
                equalTo(toolTypeKey, "finger")
            )
    }
}
