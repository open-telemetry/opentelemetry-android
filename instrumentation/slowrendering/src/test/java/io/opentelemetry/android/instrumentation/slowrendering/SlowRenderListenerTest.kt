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
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.sdk.trace.data.SpanData
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowingConsumer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.stubbing.Answer
import org.robolectric.annotation.Config
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.Stream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.N])
class SlowRenderListenerTest {
    @get:Rule
    var otelTesting: OpenTelemetryRule = OpenTelemetryRule.create()

    @get:Rule
    var mocks: MockitoRule = MockitoJUnit.rule()

    @Mock
    lateinit var frameMetricsHandler: Handler

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    lateinit var activity: Activity

    @Mock
    lateinit var frameMetrics: FrameMetrics

    @Mock
    internal lateinit var jankReporter: JankReporter
    private lateinit var executorService: ScheduledExecutorService

    @Captor
    internal lateinit var activityListenerCaptor: ArgumentCaptor<PerActivityListener>

    @Before
    fun setup() {
        executorService = Executors.newSingleThreadScheduledExecutor()
        val componentName = ComponentName("io.otel", "Komponent")
        Mockito.`when`(activity.componentName).thenReturn(componentName)
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

        Mockito
            .verify(activity.window)
            .addOnFrameMetricsAvailableListener(
                activityListenerCaptor.capture(),
                ArgumentMatchers.eq(frameMetricsHandler),
            )
        Assertions
            .assertThat(activityListenerCaptor.getValue().getActivityName())
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

        Mockito.verifyNoInteractions(activity)
        Assertions.assertThat(otelTesting.spans).hasSize(0)
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

        Mockito
            .verify(activity.window)
            .addOnFrameMetricsAvailableListener(
                activityListenerCaptor.capture(),
                ArgumentMatchers.eq(frameMetricsHandler),
            )
        Mockito
            .verify(activity.window)
            .removeOnFrameMetricsAvailableListener(activityListenerCaptor.getValue())

        Assertions.assertThat(otelTesting.spans).hasSize(0)
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

        Mockito
            .verify(activity.window)
            .addOnFrameMetricsAvailableListener(
                activityListenerCaptor.capture(),
                ArgumentMatchers.any(),
            )
        val listener = activityListenerCaptor.getValue()
        for (duration in makeSomeDurations()) {
            Mockito
                .`when`(frameMetrics.getMetric(FrameMetrics.DRAW_DURATION))
                .thenReturn(duration)
            listener.onFrameMetricsAvailable(null, frameMetrics, 0)
        }

        testInstance.onActivityPaused(activity)

        val spans = otelTesting.spans
        assertSpanContent(spans)
    }

    @Test
    fun start() {
        val exec = Mockito.mock(ScheduledExecutorService::class.java)

        Mockito
            .doAnswer(
                Answer { invocation ->
                    val runnable = invocation.getArgument<Runnable>(0)
                    runnable.run() // just call it immediately
                    null
                },
            ).`when`(exec)
            .scheduleWithFixedDelay(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(1001L),
                ArgumentMatchers.eq(1001L),
                ArgumentMatchers.eq(
                    TimeUnit.MILLISECONDS,
                ),
            )

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

        Mockito
            .verify(activity.window)
            .addOnFrameMetricsAvailableListener(
                activityListenerCaptor.capture(),
                ArgumentMatchers.any(),
            )
        val listener = activityListenerCaptor.getValue()
        for (duration in makeSomeDurations()) {
            Mockito
                .`when`(frameMetrics.getMetric(FrameMetrics.DRAW_DURATION))
                .thenReturn(duration)
            listener.onFrameMetricsAvailable(null, frameMetrics, 0)
        }

        testInstance.start()

        val spans = otelTesting.spans
        assertSpanContent(spans)
    }

    @Test
    fun activityListenerSkipsFirstFrame() {
        val listener = PerActivityListener(activity)
        Mockito
            .`when`(frameMetrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME))
            .thenReturn(1L)
        listener.onFrameMetricsAvailable(null, frameMetrics, 99)
        Mockito
            .verify(frameMetrics, Mockito.never())
            .getMetric(FrameMetrics.DRAW_DURATION)
    }

    private fun makeSomeDurations(): MutableList<Long?> =
        Stream
            .of(
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
            }.collect(Collectors.toList())

    companion object {
        private val COUNT_KEY: AttributeKey<Long?> = AttributeKey.longKey("count")

        private fun assertSpanContent(spans: MutableList<SpanData?>?) {
            Assertions
                .assertThat<SpanData?>(spans)
                .hasSize(2)
                .satisfiesExactly(
                    ThrowingConsumer { span ->
                        OpenTelemetryAssertions
                            .assertThat(span)
                            .hasName("slowRenders")
                            .endsAt(span!!.startEpochNanos)
                            .hasAttribute<Long?>(COUNT_KEY, 3L)
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
                            .hasAttribute<Long?>(COUNT_KEY, 1L)
                            .hasAttribute(
                                AttributeKey.stringKey("activity.name"),
                                "io.otel/Komponent",
                            )
                    },
                )
        }
    }
}
