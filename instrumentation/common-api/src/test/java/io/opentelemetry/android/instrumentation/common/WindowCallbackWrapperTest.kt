/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common

import android.app.Application
import android.os.Build.VERSION_CODES
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.MotionEvent
import android.view.Window
import android.view.Window.Callback
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import io.opentelemetry.android.test.common.TestActivity
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
class WindowCallbackWrapperTest {
    private lateinit var openTelemetryRule: OpenTelemetryRule

    lateinit var activityScenario: ActivityScenario<TestActivity>
    lateinit var application: Application

    @Before
    fun setUp() {
        openTelemetryRule = OpenTelemetryRule.create()
        application = ApplicationProvider.getApplicationContext()
        activityScenario = ActivityScenario.launch(TestActivity::class.java)
    }

    class TestingTracker(
        val key: String,
    ) : ToggleableTracker {
        var reference: WeakReference<Window>? = null

        override fun startTracking(window: Window) {
            reference = WeakReference(window)
            WindowCallbackWrapper.registerConsumer(
                window,
                key,
                object : TouchEventConsumer {
                    override fun consumeEvent(event: MotionEvent?) {
                    }
                },
            )
        }

        override fun stopTracking() {
            reference?.get()?.run {
                WindowCallbackWrapper.unregisterConsumer(this, key)
            }
        }
    }

    @Test
    fun callbackWrappedOnFirstResume() {
        var originalCallback: Window.Callback? = null
        activityScenario.onActivity {
            originalCallback = it.window.callback
        }
        assertNotNull(originalCallback)
        assertFalse(originalCallback is WindowCallbackWrapper)

        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(TestingTracker("tracker-a")),
        )
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(TestingTracker("tracker-b")),
        )

        var currentCallback: Window.Callback? = null
        activityScenario.recreate()
        activityScenario.onActivity {
            currentCallback = it.window.callback
        }
        assertNotNull(currentCallback)
        assertTrue(currentCallback is WindowCallbackWrapper)
        MatcherAssert.assertThat(
            (currentCallback as WindowCallbackWrapper).keys(),
            CoreMatchers.hasItems("tracker-a", "tracker-b"),
        )
    }

    @Test
    fun callbackNotWrappedOnPause() {
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(TestingTracker("tracker-a")),
        )
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(TestingTracker("tracker-b")),
        )

        var currentCallback: Window.Callback? = null

        activityScenario.recreate()
        activityScenario.onActivity {
            currentCallback = it.window.callback
        }
        assertNotNull(currentCallback)
        assertTrue(currentCallback is WindowCallbackWrapper)
        MatcherAssert.assertThat(
            (currentCallback as WindowCallbackWrapper).keys(),
            CoreMatchers.hasItems("tracker-a", "tracker-b"),
        )

        activityScenario.moveToState(Lifecycle.State.STARTED)

        activityScenario.onActivity {
            currentCallback = it.window.callback
        }
        assertNotNull(currentCallback)
        assertFalse(currentCallback is WindowCallbackWrapper)
    }

    @Test
    fun callbackWrappedOnResume() {
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(TestingTracker("tracker-a")),
        )
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(TestingTracker("tracker-b")),
        )

        var currentCallback: Window.Callback? = null

        activityScenario.recreate()
        activityScenario.onActivity {
            currentCallback = it.window.callback
        }
        assertNotNull(currentCallback)
        assertTrue(currentCallback is WindowCallbackWrapper)
        MatcherAssert.assertThat(
            (currentCallback as WindowCallbackWrapper).keys(),
            CoreMatchers.hasItems("tracker-a", "tracker-b"),
        )

        activityScenario.moveToState(Lifecycle.State.STARTED)

        activityScenario.onActivity {
            currentCallback = it.window.callback
        }
        assertNotNull(currentCallback)
        assertFalse(currentCallback is WindowCallbackWrapper)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            currentCallback = it.window.callback
        }
        assertNotNull(currentCallback)
        assertTrue(currentCallback is WindowCallbackWrapper)
        MatcherAssert.assertThat(
            (currentCallback as WindowCallbackWrapper).keys(),
            CoreMatchers.hasItems("tracker-a", "tracker-b"),
        )
    }

    @Test
    fun thirdPartyWrapperDoesNotCauseDuplicatesForWindowOnRegister() {
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(TestingTracker("tracker-a")),
        )
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(MinimalTrackerThirdParty()),
        )
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(TestingTracker("tracker-b")),
        )

        var currentCallback: Window.Callback? = null

        activityScenario.recreate()
        activityScenario.onActivity {
            currentCallback = it.window.callback
        }

        assertNotNull(currentCallback)
        assertFalse(currentCallback is WindowCallbackWrapper)

        val list = traverseWrapper(currentCallback)
        assertThat(list.count { it is WindowCallbackWrapper }, equalTo(1))
        val wrapper = list.first { it is WindowCallbackWrapper } as WindowCallbackWrapper
        assertThat(
            wrapper.keys(),
            CoreMatchers.hasItems("tracker-a", "tracker-b"),
        )
    }

    @Test
    fun thirdPartyWrapperDoesNotGetErasedOnUnregister() {
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(TestingTracker("tracker-a")),
        )
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(MinimalTrackerThirdParty()),
        )
        application.registerActivityLifecycleCallbacks(
            InstrumentationActivityCallback(TestingTracker("tracker-b")),
        )

        var currentCallback: Window.Callback? = null

        activityScenario.recreate()

        activityScenario.onActivity {
            currentCallback = it.window.callback
        }
        var list = traverseWrapper(currentCallback!!)
        assertThat(list.count { it is ThirdPartyWindowCallbackWrapper }, equalTo(1))

        activityScenario.moveToState(Lifecycle.State.STARTED)

        activityScenario.onActivity {
            currentCallback = it.window.callback
        }

        assertNotNull(currentCallback)
        assertFalse(currentCallback is WindowCallbackWrapper)

        list = traverseWrapper(currentCallback)
        assertThat(list.count { it is ThirdPartyWindowCallbackWrapper }, equalTo(1))
        val wrapper = list.first { it is WindowCallbackWrapper } as WindowCallbackWrapper
        assertThat(
            wrapper.keys().size,
            equalTo(0),
        )
    }

    fun traverseWrapper(callback: Window.Callback): List<Window.Callback> {
        var currentCallback = callback
        var wrapped = currentCallback is WindowCallbackWrapper || currentCallback is ThirdPartyWindowCallbackWrapper

        if (!wrapped) {
            return listOf(currentCallback)
        }
        val list = mutableListOf<Window.Callback>()
        while (wrapped) {
            list.add(currentCallback)
            if (currentCallback is WindowCallbackWrapper) {
                currentCallback = (currentCallback).unwrap()
            } else {
                currentCallback = (currentCallback as ThirdPartyWindowCallbackWrapper).unwrap()
            }
            wrapped = currentCallback is WindowCallbackWrapper || currentCallback is ThirdPartyWindowCallbackWrapper
        }
        list.add(currentCallback)
        return list
    }

    class ThirdPartyWindowCallbackWrapper(
        private val callback: Callback,
        private val consumer: TouchEventConsumer,
    ) : Callback by callback {
        override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
            consumer.consumeEvent(event)
            return callback.dispatchTouchEvent(event)
        }

        @RequiresApi(api = VERSION_CODES.O)
        override fun onPointerCaptureChanged(hasCapture: Boolean) {
            callback.onPointerCaptureChanged(hasCapture)
        }

        @RequiresApi(api = VERSION_CODES.N)
        override fun onProvideKeyboardShortcuts(
            data: List<KeyboardShortcutGroup?>?,
            menu: Menu?,
            deviceId: Int,
        ) {
            callback.onProvideKeyboardShortcuts(data, menu, deviceId)
        }

        fun unwrap(): Callback = callback
    }

    class MinimalTrackerThirdParty :
        TouchEventConsumer,
        ToggleableTracker {
        private var windowRef: WeakReference<Window>? = null

        override fun startTracking(window: Window) {
            windowRef = WeakReference(window)
            val currentCallback = window.callback
            window.callback = ThirdPartyWindowCallbackWrapper(currentCallback, this)
        }

        override fun consumeEvent(event: MotionEvent?) {
            windowRef?.get()?.let { window ->
            }
        }

        override fun stopTracking() {
        }
    }
}
