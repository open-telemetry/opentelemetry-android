/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.app.Application
import android.view.MotionEvent
import android.view.Window
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import io.opentelemetry.android.test.common.TestActivity
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference
import kotlin.jvm.java

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

    class TestingTracker(val key: String): ToggleableTracker {
        var reference: WeakReference<Window>? = null
        override fun startTracking(window: Window) {
            reference = WeakReference(window)
            WindowCallbackWrapper.registerConsumer(window, key, object : TouchEventConsumer {
                override fun consumeEvent(event: MotionEvent?) {

                }
            })
        }

        override fun stopTracking() {
            reference?.get()?.run {
                WindowCallbackWrapper.unregisterConsumer(this, key)
            }
        }
    }


    @Test
    fun callbackWrappedOnResume() {


        var originalCallback: Window.Callback? = null
        activityScenario.onActivity {
            originalCallback = it.window.callback
        }
        assertNotNull(originalCallback)
        assertFalse(originalCallback is WindowCallbackWrapper)

        application.registerActivityLifecycleCallbacks(
            ViewClickActivityCallback(TestingTracker("tracker-a"))
        )
        application.registerActivityLifecycleCallbacks(
            ViewClickActivityCallback(TestingTracker("tracker-b"))
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
            CoreMatchers.hasItems("tracker-a", "tracker-b")
        )
    }

    @Test
    fun callbackNotWrappedOnPause() {

        application.registerActivityLifecycleCallbacks(
            ViewClickActivityCallback(TestingTracker("tracker-a"))
        )
        application.registerActivityLifecycleCallbacks(
            ViewClickActivityCallback(TestingTracker("tracker-b"))
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
            CoreMatchers.hasItems("tracker-a", "tracker-b")
        )

        activityScenario.moveToState(Lifecycle.State.STARTED)

        activityScenario.onActivity {
            currentCallback = it.window.callback
        }
        assertNotNull(currentCallback)
        assertFalse(currentCallback is WindowCallbackWrapper)

    }

}
