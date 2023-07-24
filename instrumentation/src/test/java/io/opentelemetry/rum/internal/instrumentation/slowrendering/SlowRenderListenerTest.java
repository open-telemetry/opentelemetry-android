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

package io.opentelemetry.rum.internal.instrumentation.slowrendering;

import static android.view.FrameMetrics.DRAW_DURATION;
import static android.view.FrameMetrics.FIRST_DRAW_FRAME;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
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
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.N)
public class SlowRenderListenerTest {

    private static final AttributeKey<Long> COUNT_KEY = AttributeKey.longKey("count");

    @Rule public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    @Rule public MockitoRule mocks = MockitoJUnit.rule();

    @Mock Handler frameMetricsHandler;
    @Mock Application application;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Activity activity;

    @Mock FrameMetrics frameMetrics;
    Tracer tracer;

    @Captor ArgumentCaptor<SlowRenderListener.PerActivityListener> activityListenerCaptor;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        ComponentName componentName = new ComponentName("io.otel", "Komponent");
        when(activity.getComponentName()).thenReturn(componentName);
    }

    @Test
    public void add() {
        SlowRenderListener testInstance =
                new SlowRenderListener(tracer, null, frameMetricsHandler, Duration.ZERO);

        testInstance.onActivityResumed(activity);

        verify(activity.getWindow())
                .addOnFrameMetricsAvailableListener(
                        activityListenerCaptor.capture(), eq(frameMetricsHandler));
        assertEquals("io.otel/Komponent", activityListenerCaptor.getValue().getActivityName());
    }

    @Test
    public void removeBeforeAddOk() {
        SlowRenderListener testInstance =
                new SlowRenderListener(tracer, null, frameMetricsHandler, Duration.ZERO);

        testInstance.onActivityPaused(activity);

        verifyNoInteractions(activity);
        assertThat(otelTesting.getSpans()).hasSize(0);
    }

    @Test
    public void addAndRemove() {
        SlowRenderListener testInstance =
                new SlowRenderListener(tracer, null, frameMetricsHandler, Duration.ZERO);

        testInstance.onActivityResumed(activity);
        testInstance.onActivityPaused(activity);

        verify(activity.getWindow())
                .addOnFrameMetricsAvailableListener(
                        activityListenerCaptor.capture(), eq(frameMetricsHandler));
        verify(activity.getWindow())
                .removeOnFrameMetricsAvailableListener(activityListenerCaptor.getValue());

        assertThat(otelTesting.getSpans()).hasSize(0);
    }

    @Test
    public void removeWithMetrics() {
        SlowRenderListener testInstance =
                new SlowRenderListener(tracer, null, frameMetricsHandler, Duration.ZERO);

        testInstance.onActivityResumed(activity);

        verify(activity.getWindow())
                .addOnFrameMetricsAvailableListener(activityListenerCaptor.capture(), any());
        SlowRenderListener.PerActivityListener listener = activityListenerCaptor.getValue();
        for (long duration : makeSomeDurations()) {
            when(frameMetrics.getMetric(DRAW_DURATION)).thenReturn(duration);
            listener.onFrameMetricsAvailable(null, frameMetrics, 0);
        }

        testInstance.onActivityPaused(activity);

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

        SlowRenderListener testInstance =
                new SlowRenderListener(tracer, exec, frameMetricsHandler, Duration.ofMillis(1001));

        testInstance.onActivityResumed(activity);

        verify(activity.getWindow())
                .addOnFrameMetricsAvailableListener(activityListenerCaptor.capture(), any());
        SlowRenderListener.PerActivityListener listener = activityListenerCaptor.getValue();
        for (long duration : makeSomeDurations()) {
            when(frameMetrics.getMetric(DRAW_DURATION)).thenReturn(duration);
            listener.onFrameMetricsAvailable(null, frameMetrics, 0);
        }

        testInstance.start();

        List<SpanData> spans = otelTesting.getSpans();
        assertSpanContent(spans);
    }

    @Test
    public void activityListenerSkipsFirstFrame() {
        SlowRenderListener.PerActivityListener listener =
                new SlowRenderListener.PerActivityListener(null);
        when(frameMetrics.getMetric(FIRST_DRAW_FRAME)).thenReturn(1L);
        listener.onFrameMetricsAvailable(null, frameMetrics, 99);
        verify(frameMetrics, never()).getMetric(DRAW_DURATION);
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
