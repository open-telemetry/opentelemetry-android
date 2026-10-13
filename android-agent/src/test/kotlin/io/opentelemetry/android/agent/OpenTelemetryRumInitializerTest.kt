/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.session.SessionIdTimeoutHandler
import io.opentelemetry.android.agent.session.SessionStorage
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.sdk.testing.time.TestClock
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.time.Duration.Companion.hours

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
    fun `Verify timeoutHandler initialization 2`() {
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
            appLifecycle.registerListener(any<SessionIdTimeoutHandler>())
        }
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

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `default clock includes deep sleep in background inactivity`() {
        val listener = slot<SessionIdTimeoutHandler>()
        every { appLifecycle.registerListener(capture(listener)) } just Runs
        val rum =
            OpenTelemetryRumInitializer.initialize(RuntimeEnvironment.getApplication()) {
                diskBuffering { enabled(false) }
                httpExport { baseUrl = "http://127.0.0.1:4318" }
            }
        try {
            val first = rum.sessionProvider.getSessionId()
            listener.captured.onApplicationBackgrounded()
            val uptime = SystemClock.uptimeMillis()
            ShadowSystemClock.simulateDeepSleep(Duration.ofMinutes(14))
            assertThat(rum.sessionProvider.getSessionId()).isEqualTo(first)
            ShadowSystemClock.simulateDeepSleep(Duration.ofMinutes(1))
            assertThat(SystemClock.uptimeMillis()).isEqualTo(uptime)
            listener.captured.onApplicationForegrounded()
            val next = rum.sessionProvider.getSessionId()
            assertThat(next).isNotEqualTo(first)
            assertThat(rum.sessionProvider.getSessionId()).isEqualTo(next)
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun `span and log attribution with custom storage do not extend background inactivity`() {
        val listener = slot<SessionIdTimeoutHandler>()
        every { appLifecycle.registerListener(capture(listener)) } just Runs
        val clock = TestClock.create()
        val storage = mockk<SessionStorage>()
        val saved = Collections.synchronizedList(mutableListOf<Session>())
        every { storage.save(capture(saved)) } returns Unit
        val rum =
            OpenTelemetryRumInitializer.initialize(RuntimeEnvironment.getApplication()) {
                this.clock = clock
                session { storage(storage) }
                diskBuffering { enabled(false) }
                httpExport { baseUrl = "http://127.0.0.1:4318" }
            }
        try {
            val first = rum.sessionProvider.getSessionId()
            listener.captured.onApplicationBackgrounded()
            repeat(2) {
                clock.advance(7, MINUTES)
                val span =
                    rum.openTelemetry
                        .getTracer("test")
                        .spanBuilder("background")
                        .startSpan()
                assertThat(span.isRecording).isTrue()
                span.end()
                rum.openTelemetry.logsBridge
                    .get("test")
                    .logRecordBuilder()
                    .setBody("background")
                    .emit()
                assertThat(rum.sessionProvider.getSessionId()).isEqualTo(first)
            }
            clock.advance(1, MINUTES)
            val next = rum.sessionProvider.getSessionId()
            assertThat(next).isNotEqualTo(first)
            assertThat(saved.map { it.id }).containsExactly(first, next)
            verify(exactly = 0) { storage.get() }
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun `custom storage is used by the configured manager`() {
        val storage = mockk<SessionStorage>()
        val saved = Collections.synchronizedList(mutableListOf<Session>())
        every { storage.save(capture(saved)) } returns Unit
        val testClock = TestClock.create()
        val observer = mockk<SessionObserver>(relaxed = true)
        val rum =
            OpenTelemetryRumInitializer.initialize(RuntimeEnvironment.getApplication()) {
                clock = testClock
                disableLogging()
                disableTracing()
                disableMetrics()
                session {
                    storage(storage)
                    maxLifetime = 1.hours
                    observers(observer)
                }
            }
        try {
            val first = rum.sessionProvider.getSessionId()
            assertThat(saved.first().id).isEqualTo(first)
            assertThat(saved.last().id).isEqualTo(first)
            testClock.advance(1, TimeUnit.HOURS)
            val second = rum.sessionProvider.getSessionId()
            assertThat(second).isNotEqualTo(first)
            assertThat(saved.map { it.id }).containsExactly(first, second)
            verify { observer.onSessionStarted(match { it.id == second }, match { it.id == first }) }
            verify { appLifecycle.registerListener(any<SessionIdTimeoutHandler>()) }
            verify(exactly = 0) { storage.get() }
        } finally {
            rum.shutdown()
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
