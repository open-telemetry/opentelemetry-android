package com.splunk.rum;

import static androidx.core.app.FrameMetricsAggregator.DRAW_INDEX;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.core.app.FrameMetricsAggregator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;

@RunWith(MockitoJUnitRunner.class)
public class SlowRenderingDetectorImplTest {

    public static final AttributeKey<Long> COUNT_KEY = AttributeKey.longKey("count");
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    @Mock
    FrameMetricsAggregator frameMetrics;
    @Mock
    Activity activity;
    Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }


    @Test
    public void add() {
        SlowRenderingDetectorImpl testInstance = new SlowRenderingDetectorImpl(tracer, frameMetrics, null, Duration.ofSeconds(0));
        testInstance.add(activity);
        verify(frameMetrics).add(activity);
    }

    @Test
    public void stopBeforeAddOk() {
        SlowRenderingDetectorImpl testInstance = new SlowRenderingDetectorImpl(tracer, frameMetrics, null, Duration.ofSeconds(0));
        testInstance.stop(activity);
    }

    @Test
    public void stop() {

        SparseIntArray[] metricsArray = makeSomeMetrics();

        when(frameMetrics.remove(activity)).thenReturn(metricsArray);

        SlowRenderingDetectorImpl testInstance = new SlowRenderingDetectorImpl(tracer, frameMetrics, null, Duration.ofMillis(1001));

        testInstance.add(activity);
        testInstance.stop(activity);
        List<SpanData> spans = otelTesting.getSpans();
        assertSpanContent(spans);
    }

    @Test
    public void start() {
        SparseIntArray[] metricsArray = makeSomeMetrics();
        ScheduledExecutorService exec = mock(ScheduledExecutorService.class);

        when(frameMetrics.reset()).thenReturn(metricsArray);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run(); // just call it immediately
            return null;
        }).when(exec).scheduleAtFixedRate(any(), eq(1001L), eq(1001L), eq(TimeUnit.MILLISECONDS));

        SlowRenderingDetectorImpl testInstance = new SlowRenderingDetectorImpl(tracer, frameMetrics, exec, Duration.ofMillis(1001));
        testInstance.add(activity);
        testInstance.start();
        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());
        assertSpanContent(spans);
        InOrder inOrder = inOrder(frameMetrics);
        inOrder.verify(frameMetrics).add(activity);
        inOrder.verify(frameMetrics).remove(activity);
        inOrder.verify(frameMetrics).add(activity);
        inOrder.verifyNoMoreInteractions();
    }

    private void assertSpanContent(List<SpanData> spans) {
        assertEquals(2, spans.size());
        SpanData slowRenders = spans.get(0);
        SpanData frozenRenders = spans.get(1);

        assertEquals("slowRenders", slowRenders.getName());
        assertEquals(0, slowRenders.getStartEpochNanos() - slowRenders.getEndEpochNanos());
        long slowCount = slowRenders.getAttributes().get(COUNT_KEY);
        assertEquals(2, slowCount);

        assertEquals("frozenRenders", frozenRenders.getName());
        assertEquals(0, frozenRenders.getStartEpochNanos() - frozenRenders.getEndEpochNanos());
        long frozenCount = frozenRenders.getAttributes().get(COUNT_KEY);
        assertEquals(1, frozenCount);
    }

    @NonNull
    private SparseIntArray[] makeSomeMetrics() {
        SparseIntArray[] metricsArray = new SparseIntArray[DRAW_INDEX + 1];
        SparseIntArray drawMetrics = mock(SparseIntArray.class);
        when(drawMetrics.size()).thenReturn(3);
        addFrameMetric(drawMetrics, 0, 12, 17);
        addFrameMetric(drawMetrics, 1, 100, 2);
        addFrameMetric(drawMetrics, 2, 701, 1);

        metricsArray[DRAW_INDEX] = drawMetrics;
        return metricsArray;
    }

    private void addFrameMetric(SparseIntArray drawMetrics, int index, int key, int value) {
        when(drawMetrics.keyAt(index)).thenReturn(key);
        when(drawMetrics.get(key)).thenReturn(value);
    }

}