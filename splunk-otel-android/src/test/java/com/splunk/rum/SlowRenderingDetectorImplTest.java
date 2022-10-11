/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Build;
import android.os.Handler;
import android.view.FrameMetrics;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.N)
public class SlowRenderingDetectorImplTest {

    private static final AttributeKey<Long> COUNT_KEY = AttributeKey.longKey("count");

    @Rule public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    @Rule public MockitoRule mocks = MockitoJUnit.rule();

    @Mock Handler frameMetricsHandler;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Activity activity;

    @Mock FrameMetrics frameMetrics;
    Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        ComponentName componentName = new ComponentName("io.otel", "Komponent");
        when(activity.getComponentName()).thenReturn(componentName);
    }

    @Test
    public void add() {
        SlowRenderingDetectorImpl testInstance =
                new SlowRenderingDetectorImpl(tracer, null, frameMetricsHandler, Duration.ZERO);

        testInstance.add(activity);

        ArgumentCaptor<SlowRenderingDetectorImpl.PerActivityListener> captor =
                ArgumentCaptor.forClass(SlowRenderingDetectorImpl.PerActivityListener.class);

        verify(activity.getWindow())
                .addOnFrameMetricsAvailableListener(captor.capture(), eq(frameMetricsHandler));
        assertEquals("io.otel/Komponent", captor.getValue().getActivityName());
    }

    @Test
    public void removeBeforeAddOk() {
        SlowRenderingDetectorImpl testInstance =
                new SlowRenderingDetectorImpl(tracer, null, frameMetricsHandler, Duration.ZERO);

        testInstance.stop(activity);

        verifyNoInteractions(activity);
        assertThat(otelTesting.getSpans()).hasSize(0);
    }

    @Test
    public void addAndRemove() {
        SlowRenderingDetectorImpl testInstance =
                new SlowRenderingDetectorImpl(tracer, null, frameMetricsHandler, Duration.ZERO);

        testInstance.add(activity);
        testInstance.stop(activity);

        ArgumentCaptor<SlowRenderingDetectorImpl.PerActivityListener> captor =
                ArgumentCaptor.forClass(SlowRenderingDetectorImpl.PerActivityListener.class);

        verify(activity.getWindow())
                .addOnFrameMetricsAvailableListener(captor.capture(), eq(frameMetricsHandler));
        verify(activity.getWindow()).removeOnFrameMetricsAvailableListener(captor.getValue());
        assertThat(otelTesting.getSpans()).hasSize(0);
    }

    @Test
    public void removeWithMetrics() {
        SlowRenderingDetectorImpl testInstance =
                new SlowRenderingDetectorImpl(tracer, null, frameMetricsHandler, Duration.ZERO);

        testInstance.add(activity);

        ArgumentCaptor<SlowRenderingDetectorImpl.PerActivityListener> captor =
                ArgumentCaptor.forClass(SlowRenderingDetectorImpl.PerActivityListener.class);

        verify(activity.getWindow()).addOnFrameMetricsAvailableListener(captor.capture(), any());
        SlowRenderingDetectorImpl.PerActivityListener listener = captor.getValue();
        for (long duration : makeSomeDurations()) {
            when(frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)).thenReturn(duration);
            listener.onFrameMetricsAvailable(null, frameMetrics, 0);
        }

        testInstance.stop(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertSpanContent(spans);
    }

    @Test
    public void start() {
        ScheduledExecutorService exec = mock(ScheduledExecutorService.class);

        doAnswer(
                        invocation -> {
                            Runnable runnable = invocation.getArgument(0);
                            runnable.run(); // just call it immediately
                            return null;
                        })
                .when(exec)
                .scheduleAtFixedRate(any(), eq(1001L), eq(1001L), eq(TimeUnit.MILLISECONDS));

        SlowRenderingDetectorImpl testInstance =
                new SlowRenderingDetectorImpl(
                        tracer, exec, frameMetricsHandler, Duration.ofMillis(1001));

        testInstance.add(activity);

        ArgumentCaptor<SlowRenderingDetectorImpl.PerActivityListener> captor =
                ArgumentCaptor.forClass(SlowRenderingDetectorImpl.PerActivityListener.class);

        verify(activity.getWindow()).addOnFrameMetricsAvailableListener(captor.capture(), any());
        SlowRenderingDetectorImpl.PerActivityListener listener = captor.getValue();
        for (long duration : makeSomeDurations()) {
            when(frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)).thenReturn(duration);
            listener.onFrameMetricsAvailable(null, frameMetrics, 0);
        }

        testInstance.start();

        List<SpanData> spans = otelTesting.getSpans();
        assertSpanContent(spans);
    }

    private static void assertSpanContent(List<SpanData> spans) {
        assertThat(spans)
                .hasSize(2)
                .satisfiesExactly(
                        span ->
                                assertThat(span)
                                        .hasName("slowRenders")
                                        .endsAt(span.getStartEpochNanos())
                                        .hasAttribute(COUNT_KEY, 3L)
                                        .hasAttribute(
                                                AttributeKey.stringKey("activity.name"),
                                                "io.otel/Komponent"),
                        span ->
                                assertThat(span)
                                        .hasName("frozenRenders")
                                        .endsAt(span.getStartEpochNanos())
                                        .hasAttribute(COUNT_KEY, 1L)
                                        .hasAttribute(
                                                AttributeKey.stringKey("activity.name"),
                                                "io.otel/Komponent"));
    }

    private List<Long> makeSomeDurations() {
        return Stream.of(
                        5L, 11L, 101L, // slow
                        701L, // frozen
                        17L, // slow
                        17L, // slow
                        16L, 11L)
                .map(TimeUnit.MILLISECONDS::toNanos)
                .collect(Collectors.toList());
    }
}
