/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class SessionInputActivityTrackerTest {
    @Test
    fun `registers tracker for application contexts`() {
        val application = mockk<Application>()
        every { application.registerActivityLifecycleCallbacks(any()) } just Runs

        registerSessionInputActivityTracker(application) {}

        verify(exactly = 1) {
            application.registerActivityLifecycleCallbacks(any<SessionInputActivityTracker>())
        }
    }

    @Test
    fun `does not register tracker for non-application contexts`() {
        val context = mockk<Context>(relaxed = true)

        registerSessionInputActivityTracker(context) {}

        verify(exactly = 0) { context.applicationContext }
    }

    @Test
    fun `wraps each activity window once`() {
        val activity = mockk<Activity>()
        val window = mockk<Window>()
        val callback = mockk<Window.Callback>()
        val installedCallback = slot<Window.Callback>()
        every { activity.window } returns window
        every { window.callback } returns callback
        every { window.callback = capture(installedCallback) } just Runs
        val tracker = SessionInputActivityTracker {}

        tracker.onActivityCreated(activity, null)

        assertThat(installedCallback.captured).isInstanceOf(SessionInputWindowCallback::class.java)
    }

    @Test
    fun `touch down records activity before delegating`() {
        val calls = mutableListOf<String>()
        val callback = mockk<Window.Callback>()
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 1f, 1f, 0)
        every { callback.dispatchTouchEvent(event) } answers {
            calls += "delegate"
            true
        }
        val wrapper = SessionInputWindowCallback(callback) { calls += "activity" }

        val handled = wrapper.dispatchTouchEvent(event)

        assertThat(handled).isTrue()
        assertThat(calls).containsExactly("activity", "delegate")
        event.recycle()
    }

    @Test
    fun `touch up does not record activity`() {
        val callback = mockk<Window.Callback>()
        val recordActivity = mockk<() -> Unit>(relaxed = true)
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 1f, 1f, 0)
        every { callback.dispatchTouchEvent(event) } returns false
        val wrapper = SessionInputWindowCallback(callback, recordActivity)

        wrapper.dispatchTouchEvent(event)

        verify(exactly = 0) { recordActivity() }
        event.recycle()
    }

    @Test
    fun `initial key down records activity but repeats and key up do not`() {
        val callback = mockk<Window.Callback>()
        val recordActivity = mockk<() -> Unit>(relaxed = true)
        every { callback.dispatchKeyEvent(any()) } returns false
        val wrapper = SessionInputWindowCallback(callback, recordActivity)

        wrapper.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        wrapper.dispatchKeyEvent(KeyEvent(0, 1, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 1))
        wrapper.dispatchKeyEvent(KeyEvent(0, 2, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))

        verify(exactly = 1) { recordActivity() }
    }

    @Test
    fun `scroll records activity but other generic motion does not`() {
        val callback = mockk<Window.Callback>()
        val recordActivity = mockk<() -> Unit>(relaxed = true)
        val scrollEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_SCROLL, 1f, 1f, 0)
        val hoverEvent = MotionEvent.obtain(0, 1, MotionEvent.ACTION_HOVER_MOVE, 1f, 1f, 0)
        every { callback.dispatchGenericMotionEvent(any()) } returns false
        val wrapper = SessionInputWindowCallback(callback, recordActivity)

        wrapper.dispatchGenericMotionEvent(scrollEvent)
        wrapper.dispatchGenericMotionEvent(hoverEvent)

        verify(exactly = 1) { recordActivity() }
        scrollEvent.recycle()
        hoverEvent.recycle()
    }

    @Test
    fun `activity failure does not prevent input delegation`() {
        val callback = mockk<Window.Callback>()
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 1f, 1f, 0)
        every { callback.dispatchTouchEvent(event) } returns true
        val wrapper = SessionInputWindowCallback(callback) { error("observer failed") }

        assertThat(wrapper.dispatchTouchEvent(event)).isTrue()
        verify(exactly = 1) { callback.dispatchTouchEvent(event) }
        event.recycle()
    }
}
