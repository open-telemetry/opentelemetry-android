/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.doubletap

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.Window
import android.view.Window.Callback
import androidx.test.ext.junit.runners.AndroidJUnit4
import internal.APP_SCREEN_DOUBLE_TAP_EVENT_NAME
import internal.VIEW_DOUBLE_TAP_EVENT_NAME
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_SCREEN_COORDINATE_X
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_SCREEN_COORDINATE_Y
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_WIDGET_ID
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_WIDGET_NAME
import org.assertj.core.api.Assertions
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExtendWith(MockKExtension::class)
class DoubleTapInstrumentationTest {
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

    @Test
    fun capture_view_double_tap() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<DoubleTapActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { window.context } returns application
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        DoubleTapInstrumentation().install(installationContext)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<DoubleTapWindowCallbackWrapper>()
        every { window.callback = any() } returns Unit


        val initialDownEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis() + 100L, MotionEvent.ACTION_DOWN, 250f, 50f, 0)
        val initialUpEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + 200L, SystemClock.uptimeMillis() + 300L, MotionEvent.ACTION_UP, 250f, 50f, 0)
        val secondDownEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + 400L, SystemClock.uptimeMillis() + 500L, MotionEvent.ACTION_DOWN, 250f, 50f, 0)
        val secondUpEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + 600L, SystemClock.uptimeMillis() + 700L, MotionEvent.ACTION_UP, 250f, 50f, 0)

        val mockView = mockView<View>(10012, initialDownEvent)
        every { window.decorView } returns mockView

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(
            initialDownEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            initialUpEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            secondDownEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            secondUpEvent,
        )

        val events = openTelemetryRule.logRecords
        Assertions.assertThat(events).hasSize(2)

        var event = events[0]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(APP_SCREEN_DOUBLE_TAP_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, initialUpEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, initialUpEvent.y.toLong()),
            )

        event = events[1]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(VIEW_DOUBLE_TAP_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, mockView.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, mockView.y.toLong()),
                equalTo(APP_WIDGET_ID, mockView.id.toString()),
                equalTo(APP_WIDGET_NAME, "10012"),
            )
    }

    @Test
    fun capture_view_no_double_tap_when_timeout_exceeded() {
        val installationContext =
            InstallationContext(
                application,
                openTelemetryRule.openTelemetry,
                mockk<SessionProvider>(),
                Clock.getDefault(),
            )

        val callbackCapturingSlot = slot<DoubleTapActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { window.context } returns application
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        DoubleTapInstrumentation().install(installationContext)

        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<DoubleTapWindowCallbackWrapper>()
        every { window.callback = any() } returns Unit


        val durationOffset = 100L
        val initialDownEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis() + durationOffset * 1, MotionEvent.ACTION_DOWN, 250f, 50f, 0)
        val initialUpEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + durationOffset * 2, SystemClock.uptimeMillis() + durationOffset * 3, MotionEvent.ACTION_UP, 250f, 50f, 0)
        val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout()

        val secondDownEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + doubleTapTimeout + (durationOffset * 4), SystemClock.uptimeMillis() + doubleTapTimeout + (durationOffset * 5), MotionEvent.ACTION_DOWN, 250f, 50f, 0)
        val secondUpEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + doubleTapTimeout + (durationOffset * 6), SystemClock.uptimeMillis() + doubleTapTimeout + (durationOffset * 7), MotionEvent.ACTION_UP, 250f, 50f, 0)

        val mockView = mockView<View>(10012, initialDownEvent)
        every { window.decorView } returns mockView

        viewClickActivityCallback.onActivityResumed(activity)
        verify {
            window.callback = capture(wrapperCapturingSlot)
        }

        wrapperCapturingSlot.captured.dispatchTouchEvent(
            initialDownEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            initialUpEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            secondDownEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            secondUpEvent,
        )

        val events = openTelemetryRule.logRecords
        Assertions.assertThat(events).hasSize(0)
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

        val callbackCapturingSlot = slot<DoubleTapActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        DoubleTapInstrumentation().install(installationContext)
        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<DoubleTapWindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val initialDownEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis() + 100L, MotionEvent.ACTION_DOWN, 250f, 50f, 0)
        val initialUpEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + 200L, SystemClock.uptimeMillis() + 300L, MotionEvent.ACTION_UP, 250f, 50f, 0)
        val secondDownEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + 400L, SystemClock.uptimeMillis() + 500L, MotionEvent.ACTION_DOWN, 250f, 50f, 0)
        val secondUpEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + 600L, SystemClock.uptimeMillis() + 700L, MotionEvent.ACTION_UP, 250f, 50f, 0)

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

        wrapperCapturingSlot.captured.dispatchTouchEvent(
            initialDownEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            initialUpEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            secondDownEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            secondUpEvent,
        )

        val events = openTelemetryRule.logRecords
        Assertions.assertThat(events).hasSize(1)

        val event = events[0]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(APP_SCREEN_DOUBLE_TAP_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, initialDownEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, initialDownEvent.y.toLong()),
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

        val callbackCapturingSlot = slot<DoubleTapActivityCallback>()
        every { window.callback } returns callback
        every { callback.dispatchTouchEvent(any()) } returns false

        every { activity.window } returns window
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        DoubleTapInstrumentation().install(installationContext)
        verify {
            application.registerActivityLifecycleCallbacks(capture(callbackCapturingSlot))
        }

        val viewClickActivityCallback = callbackCapturingSlot.captured
        val wrapperCapturingSlot = slot<DoubleTapWindowCallbackWrapper>()
        every { window.callback = any() } returns Unit

        val initialDownEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis() + 100L, MotionEvent.ACTION_DOWN, 250f, 50f, 0)
        val initialUpEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + 200L, SystemClock.uptimeMillis() + 300L, MotionEvent.ACTION_UP, 250f, 50f, 0)
        val secondDownEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + 400L, SystemClock.uptimeMillis() + 500L, MotionEvent.ACTION_DOWN, 250f, 50f, 0)
        val secondUpEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis() + 600L, SystemClock.uptimeMillis() + 700L, MotionEvent.ACTION_UP, 250f, 50f, 0)

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

        wrapperCapturingSlot.captured.dispatchTouchEvent(
            initialDownEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            initialUpEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            secondDownEvent,
        )
        wrapperCapturingSlot.captured.dispatchTouchEvent(
            secondUpEvent,
        )

        val events = openTelemetryRule.logRecords
        Assertions.assertThat(events).hasSize(2)

        var event = events[0]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(APP_SCREEN_DOUBLE_TAP_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, initialDownEvent.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, initialDownEvent.y.toLong()),
            )

        event = events[1]
        OpenTelemetryAssertions.assertThat(event)
            .hasEventName(VIEW_DOUBLE_TAP_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(APP_SCREEN_COORDINATE_X, mockView.x.toLong()),
                equalTo(APP_SCREEN_COORDINATE_Y, mockView.y.toLong()),
                equalTo(APP_WIDGET_ID, mockView.id.toString()),
                equalTo(APP_WIDGET_NAME, "10012"),
            )
    }

    private inline fun <reified T : View> mockView(
        id: Int,
        motionEvent: MotionEvent,
        hitOffset: IntArray = intArrayOf(0, 0),
        clickable: Boolean = true,
        visibility: Int = View.VISIBLE,
        applyOthers: (T) -> Unit = {},
    ): T {
        val mockView = mockkClass(T::class)
        every { mockView.visibility } returns visibility
        every { mockView.isClickable } returns clickable

        every { mockView.id } returns id
        val location = IntArray(2)

        location[0] = (motionEvent.x + hitOffset[0]).toInt()
        location[1] = (motionEvent.y + hitOffset[1]).toInt()

        val arrayCapturingSlot = slot<IntArray>()
        every { mockView.getLocationInWindow(capture(arrayCapturingSlot)) } answers {
            arrayCapturingSlot.captured[0] = location[0]
            arrayCapturingSlot.captured[1] = location[1]
        }

        every { mockView.x } returns location[0].toFloat()
        every { mockView.y } returns location[1].toFloat()

        every { mockView.width } returns (location[0] + hitOffset[0])
        every { mockView.height } returns (location[1] + hitOffset[1])
        applyOthers.invoke(mockView)

        return mockView
    }
}