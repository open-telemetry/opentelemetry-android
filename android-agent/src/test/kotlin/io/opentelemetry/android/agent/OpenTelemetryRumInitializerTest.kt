/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.app.Activity
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.session.SessionInputWindowCallback
import io.opentelemetry.android.agent.session.SessionManager
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.time.TestClock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.TimeUnit

@OptIn(Incubating::class)
@RunWith(AndroidJUnit4::class)
class OpenTelemetryRumInitializerTest {
    private lateinit var appLifecycle: AppLifecycle

    @Before
    fun setUp() {
        appLifecycle = mockk(relaxed = true)
        createAndSetServiceManager()
    }

    @After
    fun tearDown() {
        Services.set(null)
    }

    @Test
    fun `registers session manager for application lifecycle events`() {
        val rum =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    httpExport {
                        baseUrl = "http://127.0.0.1:4318"
                    }
                },
            )
        rum.shutdown()

        verify {
            appLifecycle.registerListener(any<SessionManager>())
        }
    }

    @Test
    fun `tracks input for activities created after initialization`() {
        val rum =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    httpExport {
                        baseUrl = "http://127.0.0.1:4318"
                    }
                },
            )
        val activityController = Robolectric.buildActivity(Activity::class.java).create()

        assertThat(activityController.get().window.callback)
            .isInstanceOf(SessionInputWindowCallback::class.java)

        rum.shutdown()
        assertThat(activityController.get().window.callback)
            .isNotInstanceOf(SessionInputWindowCallback::class.java)
        activityController.destroy()
    }

    @Test
    fun `tracks an activity initialized before its first resume`() {
        val activityController = Robolectric.buildActivity(Activity::class.java).create()
        val rum =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    httpExport {
                        baseUrl = "http://127.0.0.1:4318"
                    }
                },
            )

        activityController.start().resume()

        assertThat(activityController.get().window.callback)
            .isInstanceOf(SessionInputWindowCallback::class.java)

        rum.shutdown()
        activityController.pause().stop().destroy()
    }

    @Test
    fun `default input tracking keeps an active session alive`() {
        val clock = TestClock.create()
        val rum =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    this.clock = clock
                    httpExport {
                        baseUrl = "http://127.0.0.1:4318"
                    }
                },
            )
        val activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val initialSessionId = rum.sessionProvider.getSessionIdForAttribution()

        repeat(4) {
            clock.advance(10, TimeUnit.MINUTES)
            val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 1f, 1f, 0)
            activityController
                .get()
                .window.callback
                .dispatchTouchEvent(event)
            event.recycle()
        }

        assertThat(rum.sessionProvider.getSessionIdForAttribution()).isEqualTo(initialSessionId)

        rum.shutdown()
        activityController.pause().stop().destroy()
    }

    @Test
    fun `Verify session observers are applied`() {
        val o1: SessionObserver = mockk()
        val o2: SessionObserver = mockk()
        every { o1.onSessionStarted(any(), any()) } just Runs
        every { o1.onSessionEnded(any()) } just Runs
        every { o2.onSessionStarted(any(), any()) } just Runs
        every { o2.onSessionEnded(any()) } just Runs

        val rum =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    httpExport {
                        baseUrl = "http://127.0.0.1:4318"
                    }
                    session {
                        observers(o1, o2)
                    }
                },
            )
        rum.shutdown()

        verify {
            o1.onSessionStarted(any(), any())
            o2.onSessionStarted(any(), any())
        }
    }

    private fun createAndSetServiceManager(): Services {
        val services = mockk<Services>()
        every { services.appLifecycle }.returns(appLifecycle)
        every { services.visibleScreenTracker }.returns(mockk(relaxed = true))
        every { services.close() } just Runs
        Services.set(services)
        return services
    }
}
