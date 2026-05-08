/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.app.Activity
import android.content.ComponentName
import android.os.Build
import android.os.Handler
import android.view.FrameMetrics
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.CapturingSlot
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import kotlinx.coroutines.Runnable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.N])
class SlowRenderListenerTest {
    @get:Rule
    var otelTesting: OpenTelemetryRule = OpenTelemetryRule.create()

    @get:Rule
    var mocks = MockKRule(this)

    @MockK
    lateinit var frameMetricsHandler: Handler

    @RelaxedMockK
    lateinit var activity: Activity

    @RelaxedMockK
    lateinit var frameMetrics: FrameMetrics

    @RelaxedMockK
    internal lateinit var jankReporter: JankReporter
    private lateinit var executorService: ScheduledExecutorService

    internal lateinit var activityListenerCaptor: CapturingSlot<PerActivityListener>

    @Before
    fun setup() {
        activityListenerCaptor = slot()
        executorService = Executors.newSingleThreadScheduledExecutor()
        val componentName = ComponentName("io.otel", "Komponent")
        every { activity.componentName } returns componentName
    }

    @Test
    fun add() {
        val testInstance =
            SlowRenderListener(
                jankReporter,
                executorService,
                frameMetricsHandler,
                Duration.ZERO,
            )

        testInstance.onActivityResumed(activity)

        verify {
            activity.window.addOnFrameMetricsAvailableListener(
                capture(activityListenerCaptor),
                eq(frameMetricsHandler),
            )
        }

        assertThat(activityListenerCaptor.captured.getActivityName())
            .isEqualTo("io.otel/Komponent")
    }

    @Test
    fun removeBeforeAddOk() {
        val testInstance =
            SlowRenderListener(
                jankReporter,
                executorService,
                frameMetricsHandler,
                Duration.ZERO,
            )

        testInstance.onActivityPaused(activity)

        confirmVerified(activity)
        assertThat(otelTesting.spans).hasSize(0)
    }

    @Test
    fun addAndRemove() {
        val testInstance =
            SlowRenderListener(
                jankReporter,
                executorService,
                frameMetricsHandler,
                Duration.ZERO,
            )

        testInstance.onActivityResumed(activity)
        testInstance.onActivityPaused(activity)

        verify {
            activity.window.addOnFrameMetricsAvailableListener(
                capture(activityListenerCaptor),
                eq(frameMetricsHandler),
            )
        }
        verify { activity.window.removeOnFrameMetricsAvailableListener(eq(activityListenerCaptor.captured)) }

        assertThat(otelTesting.spans).hasSize(0)
    }

    @Test
    fun removeWithMetrics() {
        jankReporter = createEventReporter()
        val testInstance =
            SlowRenderListener(
                jankReporter,
                executorService,
                frameMetricsHandler,
                Duration.ZERO,
            )

        testInstance.onActivityResumed(activity)

        verify {
            activity.window.addOnFrameMetricsAvailableListener(
                capture(activityListenerCaptor),
                any(),
            )
        }
        val listener = activityListenerCaptor.captured
        for (duration in makeSomeDurations()) {
            every { frameMetrics.getMetric(FrameMetrics.DRAW_DURATION) } returns duration
            listener.onFrameMetricsAvailable(null, frameMetrics, 0)
        }

        testInstance.onActivityPaused(activity)

        assertLogContent(periodSeconds = 0.0)
    }

    @Test
    fun start() {
        val exec = mockk<ScheduledExecutorService>(relaxed = true)
        every {
            exec.scheduleWithFixedDelay(
                any(),
                eq(1001L),
                eq(1001L),
                eq(TimeUnit.MILLISECONDS),
            )
        } answers {
            val runnable = invocation.args[0] as Runnable
            runnable.run() // just call it immediately
            null
        }

        jankReporter = createEventReporter()
        val testInstance =
            SlowRenderListener(
                jankReporter,
                exec,
                frameMetricsHandler,
                Duration.ofMillis(1001),
            )

        testInstance.onActivityResumed(activity)

        verify {
            activity.window.addOnFrameMetricsAvailableListener(
                capture(activityListenerCaptor),
                any(),
            )
        }
        val listener = activityListenerCaptor.captured
        for (duration in makeSomeDurations()) {
            every { frameMetrics.getMetric(FrameMetrics.DRAW_DURATION) } returns duration
            listener.onFrameMetricsAvailable(null, frameMetrics, 0)
        }

        testInstance.start()

        assertLogContent(periodSeconds = 1.0)
    }

    @Test
    fun activityListenerSkipsFirstFrame() {
        val listener = PerActivityListener(activity)
        every { frameMetrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) } returns 1L
        listener.onFrameMetricsAvailable(null, frameMetrics, 99)
        every { frameMetrics.getMetric(FrameMetrics.DRAW_DURATION) }
        verify(exactly = 0) { frameMetrics.getMetric(FrameMetrics.DRAW_DURATION) }
    }

    private fun makeSomeDurations(): List<Long> =
        listOf(
            5L,
            11L,
            101L, // slow
            701L, // frozen
            17L, // slow
            17L, // slow
            16L,
            11L,
        ).map { duration ->
            TimeUnit.MILLISECONDS.toNanos(
                duration,
            )
        }

    private fun createEventReporter(): JankReporter {
        val eventLogger: Logger = otelTesting.openTelemetry.logsBridge.get("JANK!")
        return EventJankReporter(eventLogger, SLOW_THRESHOLD_MS / 1000.0)
            .combine(EventJankReporter(eventLogger, FROZEN_THRESHOLD_MS / 1000.0))
    }

    private fun assertLogContent(periodSeconds: Double) {
        assertThat(otelTesting.logRecords).hasSize(2)

        val slowLog = otelTesting.logRecords[0]
        assertThat(slowLog.eventName).isEqualTo("app.jank")
        assertThat(slowLog.attributes.get(FRAME_COUNT)).isEqualTo(4)
        assertThat(slowLog.attributes.get(PERIOD)).isEqualTo(periodSeconds)
        assertThat(slowLog.attributes.get(THRESHOLD)).isEqualTo(0.016)

        val frozenLog = otelTesting.logRecords[1]
        assertThat(frozenLog.eventName).isEqualTo("app.jank")
        assertThat(frozenLog.attributes.get(FRAME_COUNT)).isEqualTo(1)
        assertThat(frozenLog.attributes.get(PERIOD)).isEqualTo(periodSeconds)
        assertThat(frozenLog.attributes.get(THRESHOLD)).isEqualTo(0.7)
    }
}
