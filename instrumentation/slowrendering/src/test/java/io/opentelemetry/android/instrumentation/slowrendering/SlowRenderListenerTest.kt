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
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.sdk.trace.data.SpanData
import kotlinx.coroutines.Runnable
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
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
        val tracer = otelTesting.openTelemetry.getTracer("testTracer")
        jankReporter = SpanBasedJankReporter(tracer)
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

        val spans = otelTesting.spans
        assertSpanContent(spans)
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

        val tracer = otelTesting.openTelemetry.getTracer("testTracer")
        jankReporter = SpanBasedJankReporter(tracer)
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

        val spans = otelTesting.spans
        assertSpanContent(spans)
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

    companion object {
        private val COUNT_KEY: AttributeKey<Long> = AttributeKey.longKey("count")

        private fun assertSpanContent(spans: MutableList<SpanData?>) {
            assertThat<SpanData>(spans)
                .hasSize(2)
                .satisfiesExactly(
                    ThrowingConsumer { span ->
                        OpenTelemetryAssertions
                            .assertThat(span)
                            .hasName("slowRenders")
                            .endsAt(span!!.startEpochNanos)
                            .hasAttribute(COUNT_KEY, 3L)
                            .hasAttribute(
                                AttributeKey.stringKey("activity.name"),
                                "io.otel/Komponent",
                            )
                    },
                    ThrowingConsumer { span ->
                        OpenTelemetryAssertions
                            .assertThat(span)
                            .hasName("frozenRenders")
                            .endsAt(span!!.startEpochNanos)
                            .hasAttribute(COUNT_KEY, 1L)
                            .hasAttribute(
                                AttributeKey.stringKey("activity.name"),
                                "io.otel/Komponent",
                            )
                    },
                )
        }
    }
}
