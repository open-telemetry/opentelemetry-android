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
import io.opentelemetry.android.Incubating
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
import org.robolectric.annotation.Config
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
@Config(sdk = [23, 36])
@OptIn(Incubating::class)
class SessionInteractionInstrumentationTest {
    private val clock = TestClock.create()
    private val config = SessionConfig(userInactivityTimeout = 1.minutes)
    private val timeout = SessionIdTimeoutHandler(config, clock)
    private val manager = SessionManager.create(timeout, config, clock)
    private val lifecycle = mockk<AppLifecycle>(relaxed = true)
    private val rum = mockk<OpenTelemetryRum>()
    private val delegate = mockk<Window.Callback>(relaxed = true)
    private val app = RuntimeEnvironment.getApplication()
    private var instrumentation = SessionInteractionInstrumentation(manager, lifecycle)
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
                    assertThat(timeout.hasTimedOut()).isFalse()
                    observed.add(manager.getSessionId())
                    return true
                }
            }
        activity.window.callback = delegate
        startTracking()
        val first = manager.getSessionId()
        clock.advance(50, SECONDS)
        repeat(120) {
            clock.advance(1, MILLISECONDS)
            touch(MotionEvent.ACTION_MOVE)
        }
        clock.advance(50, SECONDS)
        assertThat(manager.getSessionId()).isEqualTo(first)
        clock.advance(10, SECONDS)
        touch()
        val second = manager.getSessionId()
        assertThat(second).isNotEqualTo(first)
        assertThat(observed.dropLast(1)).hasSize(120).containsOnly(first)
        assertThat(observed.last()).isEqualTo(second)
    }

    @Test
    fun `keyboard activity and foreground return check expiry before extending it`() {
        activity.window.decorView
        startTracking()
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
    fun `paused or destroyed window leaves retained input inactive and other windows active`() {
        val recorder = mockk<SessionUserInteractionRecorder>(relaxed = true)
        activity.window.callback = delegate
        startTracking(recorder)
        val retained = activity.window.callback
        val other = Robolectric.buildActivity(Activity::class.java).create().get()
        other.window.callback = delegate
        instrumentation.onActivityResumed(other)
        instrumentation.onActivityPaused(activity)
        val key = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)
        retained.dispatchKeyEvent(key)
        verify(exactly = 0) { recorder.recordUserInteraction() }
        instrumentation.onActivityResumed(activity)
        instrumentation.onActivityDestroyed(activity)
        assertThat(activity.window.callback).isSameAs(delegate)
        retained.dispatchKeyEvent(key)
        verify(exactly = 0) { recorder.recordUserInteraction() }
        other.window.callback.dispatchKeyEvent(key)
        verify(exactly = 1) { recorder.recordUserInteraction() }
        verify(exactly = 3) { delegate.dispatchKeyEvent(key) }
        instrumentation.onActivityDestroyed(other)
        assertThat(other.window.callback).isSameAs(delegate)
    }

    @Test
    fun `session callback exception does not prevent delivery to the application`() {
        activity.window.callback = delegate
        startTracking { error("observer failed") }
        touch()
        verify(exactly = 1) { delegate.dispatchTouchEvent(any()) }
        instrumentation.uninstall(app, rum)
        assertThat(activity.window.callback).isSameAs(delegate)
    }

    @Test
    fun `resume skips a missing callback and can track its replacement`() {
        instrumentation.install(app, rum)
        val original = activity.window.callback
        activity.window.callback = null
        instrumentation.onActivityResumed(activity)
        assertThat(activity.window.callback).isNull()
        activity.window.callback = original
        instrumentation.onActivityResumed(activity)
        assertThat(activity.window.callback).isNotSameAs(original)
    }

    @Test
    fun `shutdown leaves later callback wrappers intact and makes retained callbacks inert`() {
        activity.window.callback = delegate
        startTracking()
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
        activity.window.callback = delegate
        startTracking(recorder)
        val previous = activity.window.callback
        instrumentation.onActivityPaused(activity)
        activity.window.callback = object : Window.Callback by previous {}
        instrumentation.onActivityResumed(activity)
        touch()
        verify(exactly = 1) { recorder.recordUserInteraction() }
        verify(exactly = 1) { delegate.dispatchTouchEvent(any()) }
        val replacement = mockk<Window.Callback>(relaxed = true)
        activity.window.callback = replacement
        instrumentation.onActivityResumed(activity)
        touch()
        verify(exactly = 2) { recorder.recordUserInteraction() }
        verify(exactly = 1) { replacement.dispatchTouchEvent(any()) }
    }

    @Test
    fun `move and scroll record activity but release and hover only delegate`() {
        val recorder = mockk<SessionUserInteractionRecorder>(relaxed = true)
        activity.window.callback = delegate
        startTracking(recorder)
        val actions = listOf(MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP, MotionEvent.ACTION_SCROLL, MotionEvent.ACTION_HOVER_MOVE)
        actions.forEach { action ->
            touch(action, generic = action == MotionEvent.ACTION_SCROLL || action == MotionEvent.ACTION_HOVER_MOVE)
        }
        verify(exactly = 2) { recorder.recordUserInteraction() }
        verify(exactly = 2) { delegate.dispatchTouchEvent(any()) }
        verify(exactly = 2) { delegate.dispatchGenericMotionEvent(any()) }
    }

    @Test
    fun `background-thread shutdown disables input before main-thread cleanup`() {
        activity.window.callback = delegate
        startTracking()
        manager.getSessionId()
        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit { instrumentation.uninstall(app, rum) }.get(5, SECONDS)
            clock.advance(60, SECONDS)
            touch()
            assertThat(timeout.hasTimedOut()).isTrue()
            verify(exactly = 1) { delegate.dispatchTouchEvent(any()) }
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(activity.window.callback).isSameAs(delegate)
        } finally {
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue()
        }
    }

    private fun startTracking(recorder: SessionUserInteractionRecorder = manager) {
        instrumentation = SessionInteractionInstrumentation(recorder, lifecycle)
        instrumentation.install(app, rum)
        instrumentation.onActivityResumed(activity)
        val callback = activity.window.callback
        instrumentation.onActivityPaused(activity)
        instrumentation.onActivityResumed(activity)
        assertThat(activity.window.callback).isSameAs(callback)
    }

    private fun touch(
        action: Int = MotionEvent.ACTION_DOWN,
        generic: Boolean = false,
    ) {
        val event = MotionEvent.obtain(0, 0, action, 1f, 1f, 0)
        try {
            if (generic) activity.window.callback.dispatchGenericMotionEvent(event) else activity.window.callback.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
    }
}
