/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(IncubatingApi::class)

package io.opentelemetry.android.instrumentation.view.scroll

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.Window
import android.view.Window.Callback
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.test.common.getScrollSequence
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.internal.APP_SCREEN_FLING_EVENT_NAME
import io.opentelemetry.android.instrumentation.internal.APP_SCREEN_SCROLL_EVENT_NAME
import io.opentelemetry.android.instrumentation.internal.HARDWARE_POINTER_DISTANCE_X
import io.opentelemetry.android.instrumentation.internal.HARDWARE_POINTER_DISTANCE_Y
import io.opentelemetry.android.instrumentation.internal.HARDWARE_POINTER_TYPE
import io.opentelemetry.android.instrumentation.internal.HARDWARE_POINTER_VELOCITY_X
import io.opentelemetry.android.instrumentation.internal.HARDWARE_POINTER_VELOCITY_Y
import io.opentelemetry.android.instrumentation.internal.InternalViewApi
import io.opentelemetry.android.instrumentation.internal.VIEW_FLING_EVENT_NAME
import io.opentelemetry.android.instrumentation.internal.VIEW_SCROLL_EVENT_NAME
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey.doubleKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_COORDINATE_X
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_COORDINATE_Y
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_WIDGET_ID
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_WIDGET_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.android.test.common.mockView
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.RunWith

@InternalViewApi
@RunWith(AndroidJUnit4::class)
@ExtendWith(MockKExtension::class)
class ViewScrollInstrumentationTest {

    private lateinit var openTelemetryRule: OpenTelemetryRule

    @MockK
    lateinit var window: Window

    @MockK
    lateinit var callback: Callback

    @MockK
    lateinit var activity: Activity

    @MockK
    lateinit var application: Application


    private val toolTypeKey = stringKey(HARDWARE_POINTER_TYPE)

    @Before
    fun setUp() {
        openTelemetryRule = OpenTelemetryRule.create()
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun capture_view_scroll() {
        val openTelemetryRum = mockk<OpenTelemetryRum> {
            every { openTelemetry } returns openTelemetryRule.openTelemetry
            every { sessionProvider } returns mockk<SessionProvider>()
            every { clock } returns Clock.getDefault()
        }

        val callbackCapturingSlot = slot<ViewScrollActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewScrollInstrumentation().install(application, openTelemetryRum)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val distance = 20
        val scrollSequence = getScrollSequence(250f, 50f, 30.0, distance)
        val motionEvent = scrollSequence[0]
        val newPositionEvent = scrollSequence[1]

        val mockView = mockView<View>(10012, motionEvent)
        every { window.decorView } returns mockView

        viewClickActivityCallback.onActivityResumed(activity)

        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(scrollSequence[0])
        wrapperCapturingSlot.captured.dispatchTouchEvent(scrollSequence[1])


        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)
        val horizontalDistance = -17.0 // 17.32 rounded down
        val verticalDistance = -10.0

        var event = events[0]
        assertThat(event)
            .hasEventName(APP_SCREEN_SCROLL_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(longKey(APP_SCREEN_COORDINATE_X), newPositionEvent.x.toLong()),
                equalTo(longKey(APP_SCREEN_COORDINATE_Y), newPositionEvent.y.toLong()),
                equalTo(doubleKey(HARDWARE_POINTER_DISTANCE_X), horizontalDistance),
                equalTo(doubleKey(HARDWARE_POINTER_DISTANCE_Y), verticalDistance),
                equalTo(toolTypeKey, "finger")
            )

        event = events[1]
        assertThat(event)
            .hasEventName(VIEW_SCROLL_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(longKey(APP_SCREEN_COORDINATE_X), mockView.x.toLong()),
                equalTo(longKey(APP_SCREEN_COORDINATE_Y), mockView.y.toLong()),
                equalTo(stringKey(APP_WIDGET_ID), mockView.id.toString()),
                equalTo(stringKey(APP_WIDGET_NAME), "10012"),
                equalTo(toolTypeKey, "finger"),
                equalTo(doubleKey(HARDWARE_POINTER_DISTANCE_X), horizontalDistance),
                equalTo(doubleKey(HARDWARE_POINTER_DISTANCE_Y), verticalDistance)
            )
    }

    @Test
    fun capture_view_no_scroll_when_less_than_slop() {
        val openTelemetryRum = mockk<OpenTelemetryRum> {
            every { openTelemetry } returns openTelemetryRule.openTelemetry
            every { sessionProvider } returns mockk<SessionProvider>()
            every { clock } returns Clock.getDefault()
        }

        val callbackCapturingSlot = slot<ViewScrollActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewScrollInstrumentation().install(application, openTelemetryRum)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val touchSlop = ViewConfiguration.getTouchSlop()
        val distanceLessThanTouchSlop = (touchSlop * 0.6).toInt()
        val scrollSequence = getScrollSequence(250f, 50f, 90.0, distanceLessThanTouchSlop)
        val motionEvent = scrollSequence[0]
        val newPositionEvent = scrollSequence[1]

        val mockView = mockView<View>(10012, motionEvent)
        every { window.decorView } returns mockView

        viewClickActivityCallback.onActivityResumed(activity)

        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(
            scrollSequence[0],
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            scrollSequence[1],
        )


        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(0)
    }

    @Test
    fun capture_view_fling() {
        val openTelemetryRum = mockk<OpenTelemetryRum> {
            every { openTelemetry } returns openTelemetryRule.openTelemetry
            every { sessionProvider } returns mockk<SessionProvider>()
            every { clock } returns Clock.getDefault()
        }

        val callbackCapturingSlot = slot<ViewScrollActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewScrollInstrumentation().install(application, openTelemetryRum)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit


        val distance = 20
        val time = 100L
        val scrollSequence = getScrollSequence(250f, 50f, 90.0, distance, timeMillis = time, letGo = true)
        val motionEvent = scrollSequence[0]
        val newPositionEvent = scrollSequence[1]

        val mockView = mockView<View>(10012, motionEvent)
        every { window.decorView } returns mockView

        viewClickActivityCallback.onActivityResumed(activity)

        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(scrollSequence[0])
        wrapperCapturingSlot.captured.dispatchTouchEvent(scrollSequence[1])
        wrapperCapturingSlot.captured.dispatchTouchEvent(scrollSequence[2])


        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(4)

        val verticalVelocity = distance / (time.toDouble() / 1000)


        var event = events[2]

        assertThat(event)
            .hasEventName(APP_SCREEN_FLING_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(longKey(APP_SCREEN_COORDINATE_X), newPositionEvent.x.toLong()),
                equalTo(longKey(APP_SCREEN_COORDINATE_Y), newPositionEvent.y.toLong()),
                equalTo(doubleKey(HARDWARE_POINTER_VELOCITY_X), 0.0),
                equalTo(doubleKey(HARDWARE_POINTER_VELOCITY_Y), verticalVelocity),
                equalTo(toolTypeKey, "finger")
            )

        event = events[3]
        assertThat(event)
            .hasEventName(VIEW_FLING_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(longKey(APP_SCREEN_COORDINATE_X), mockView.x.toLong()),
                equalTo(longKey(APP_SCREEN_COORDINATE_Y), mockView.y.toLong()),
                equalTo(stringKey(APP_WIDGET_ID), mockView.id.toString()),
                equalTo(stringKey(APP_WIDGET_NAME), "10012"),
                equalTo(doubleKey(HARDWARE_POINTER_VELOCITY_X), 0.0),
                equalTo(doubleKey(HARDWARE_POINTER_VELOCITY_Y), verticalVelocity),
                equalTo(toolTypeKey, "finger")
            )
    }

    @Test
    fun capture_view_scroll_in_view_group() {
        val openTelemetryRum = mockk<OpenTelemetryRum> {
            every { openTelemetry } returns openTelemetryRule.openTelemetry
            every { sessionProvider } returns mockk<SessionProvider>()
            every { clock } returns Clock.getDefault()
        }

        val callbackCapturingSlot = slot<ViewScrollActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        ViewScrollInstrumentation().install(application, openTelemetryRum)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<WindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val distance = 20
        val scrollSequence = getScrollSequence(250f, 50f, 270.0, distance)
        val motionEvent = scrollSequence[0]
        val newPositionEvent = scrollSequence[1]

        val mockView = mockView<View>(10012, motionEvent)
        val mockViewGroup =
            mockView<ViewGroup>(
                10013,
                motionEvent,
                clickable = false
            ) {
                every { it.childCount } returns 1
                every { it.getChildAt(any()) } returns mockView
            }
        every { window.decorView } returns mockViewGroup

        viewClickActivityCallback.onActivityResumed(activity)

        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(scrollSequence[0])
        wrapperCapturingSlot.captured.dispatchTouchEvent(scrollSequence[1])


        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(1)

        var event = events[0]
        assertThat(event)
            .hasEventName(APP_SCREEN_SCROLL_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(longKey(APP_SCREEN_COORDINATE_X), newPositionEvent.x.toLong()),
                equalTo(longKey(APP_SCREEN_COORDINATE_Y), newPositionEvent.y.toLong()),
                equalTo(doubleKey(HARDWARE_POINTER_DISTANCE_X), 0.0),
                equalTo(doubleKey(HARDWARE_POINTER_DISTANCE_Y), distance.toDouble()),
                equalTo(toolTypeKey, "finger")
            )

    }

}
