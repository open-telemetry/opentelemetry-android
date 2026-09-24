/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import android.app.Activity
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.sdk.testing.time.TestClock
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class SessionInteractionInstrumentationTest {
    private val clock = TestClock.create()
    private val config = SessionConfig(userInactivityTimeout = 1.minutes)
    private val timeout = SessionIdTimeoutHandler(config, clock)
    private val manager = SessionManager.create(timeout, config, clock)
    private val lifecycle = mockk<AppLifecycle>(relaxed = true)
    private val rum = mockk<OpenTelemetryRum>()
    private val app = RuntimeEnvironment.getApplication()
    private val instrumentation = SessionInteractionInstrumentation(manager, lifecycle)
    private val activity = Robolectric.buildActivity(Activity::class.java).create().get()

    @After
    fun cleanup() {
        instrumentation.uninstall(app, rum)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `touch activity renews inactivity before application telemetry and passive reads do not`() {
        val observed = mutableListOf<String>()
        val delegate =
            object : Window.Callback by activity.window.callback {
                override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                    observed.add(manager.getSessionId())
                    return true
                }
            }
        activity.window.callback = delegate
        instrumentation.install(app, rum)
        instrumentation.onActivityResumed(activity)
        val first = manager.getSessionId()
        clock.advance(50, SECONDS)
        touch()
        clock.advance(50, SECONDS)
        assertThat(manager.getSessionId()).isEqualTo(first)
        clock.advance(10, SECONDS)
        touch()
        val second = manager.getSessionId()
        assertThat(second).isNotEqualTo(first)
        assertThat(observed).containsExactly(first, second)
    }

    @Test
    fun `keyboard activity and foreground return check expiry before extending it`() {
        activity.window.decorView
        instrumentation.install(app, rum)
        instrumentation.onActivityResumed(activity)
        val first = manager.getSessionId()
        clock.advance(60, SECONDS)
        activity.window.callback.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A))
        val second = manager.getSessionId()
        assertThat(second).isNotEqualTo(first)
        clock.advance(50, SECONDS)
        activity.window.callback.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A))
        clock.advance(10, SECONDS)
        timeout.onApplicationBackgrounded()
        timeout.onApplicationForegrounded()
        instrumentation.onApplicationForegrounded()
        assertThat(manager.getSessionId()).isNotEqualTo(second)
        assertThat(timeout.hasTimedOut()).isFalse()
    }

    @Test
    fun `paused window input delegates without refreshing inactivity`() {
        val delegate = mockk<Window.Callback>(relaxed = true)
        activity.window.callback = delegate
        instrumentation.install(app, rum)
        instrumentation.onActivityResumed(activity)
        manager.getSessionId()
        instrumentation.onActivityPaused(activity)
        clock.advance(60, SECONDS)
        touch()
        assertThat(timeout.hasTimedOut()).isTrue()
        verify { delegate.dispatchTouchEvent(any()) }
    }

    @Test
    fun `session callback failure never prevents delivery to the application`() {
        val delegate = mockk<Window.Callback>(relaxed = true)
        activity.window.callback = delegate
        val failing = SessionInteractionInstrumentation(SessionUserInteractionRecorder { error("observer failed") }, lifecycle)
        failing.install(app, rum)
        try {
            failing.onActivityResumed(activity)
            touch()
            verify(exactly = 1) { delegate.dispatchTouchEvent(any()) }
        } finally {
            failing.uninstall(app, rum)
        }
        assertThat(activity.window.callback).isSameAs(delegate)
    }

    @Test
    fun `shutdown leaves later callback wrappers intact and makes retained callbacks inert`() {
        val delegate = mockk<Window.Callback>(relaxed = true)
        activity.window.callback = delegate
        instrumentation.install(app, rum)
        instrumentation.onActivityResumed(activity)
        val installed = activity.window.callback
        val later = object : Window.Callback by installed {}
        activity.window.callback = later
        manager.getSessionId()
        instrumentation.uninstall(app, rum)
        assertThat(activity.window.callback).isSameAs(later)
        clock.advance(60, SECONDS)
        touch()
        assertThat(timeout.hasTimedOut()).isTrue()
        verify { delegate.dispatchTouchEvent(any()) }
        verify { lifecycle.unregisterListener(instrumentation) }
    }

    @Test
    fun `resume follows a replaced callback and leaves the previous wrapper inactive`() {
        val recorder = mockk<SessionUserInteractionRecorder>(relaxed = true)
        val tracking = SessionInteractionInstrumentation(recorder, lifecycle)
        val delegate = mockk<Window.Callback>(relaxed = true)
        activity.window.callback = delegate
        tracking.install(app, rum)
        try {
            tracking.onActivityResumed(activity)
            val previous = activity.window.callback
            tracking.onActivityPaused(activity)
            activity.window.callback = object : Window.Callback by previous {}
            tracking.onActivityResumed(activity)
            touch()
            verify(exactly = 1) { recorder.recordUserInteraction() }
            verify(exactly = 1) { delegate.dispatchTouchEvent(any()) }
            val replacement = mockk<Window.Callback>(relaxed = true)
            activity.window.callback = replacement
            tracking.onActivityResumed(activity)
            touch()
            verify(exactly = 2) { recorder.recordUserInteraction() }
            verify(exactly = 1) { replacement.dispatchTouchEvent(any()) }
        } finally {
            tracking.uninstall(app, rum)
        }
    }

    @Test
    fun `shutdown restores only the callback installed by this instrumentation`() {
        val original = activity.window.callback
        instrumentation.install(app, rum)
        instrumentation.onActivityResumed(activity)
        val firstWrapper = activity.window.callback
        instrumentation.onActivityPaused(activity)
        instrumentation.onActivityResumed(activity)
        assertThat(activity.window.callback).isSameAs(firstWrapper)
        instrumentation.uninstall(app, rum)
        assertThat(activity.window.callback).isSameAs(original)
    }

    private fun touch() {
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 1f, 1f, 0)
        try {
            activity.window.callback.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
    }
}
