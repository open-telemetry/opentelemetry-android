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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;

public class ActivityTracerTest {
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;
    private final VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);
    private final AppStartupTimer appStartupTimer = new AppStartupTimer();

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void restart_nonInitialActivity() {
        ActivityTracer trackableTracer = new ActivityTracer(mock(Activity.class), new AtomicReference<>("FirstActivity"), tracer, visibleScreenTracker, appStartupTimer);
        trackableTracer.initiateRestartSpanIfNecessary(false);
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Restarted", span.getName());
        assertNull(span.getAttributes().get(SplunkRum.START_TYPE_KEY));
    }

    @Test
    public void restart_initialActivity() {
        ActivityTracer trackableTracer = new ActivityTracer(mock(Activity.class), new AtomicReference<>("Activity"), tracer, visibleScreenTracker, appStartupTimer);
        trackableTracer.initiateRestartSpanIfNecessary(false);
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("AppStart", span.getName());
        assertEquals("hot", span.getAttributes().get(SplunkRum.START_TYPE_KEY));
        assertEquals(SplunkRum.COMPONENT_APPSTART, span.getAttributes().get(SplunkRum.COMPONENT_KEY));
    }

    @Test
    public void restart_initialActivity_multiActivityApp() {
        ActivityTracer trackableTracer = new ActivityTracer(mock(Activity.class), new AtomicReference<>("Activity"), tracer, visibleScreenTracker, appStartupTimer);
        trackableTracer.initiateRestartSpanIfNecessary(true);
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Restarted", span.getName());
        assertNull(span.getAttributes().get(SplunkRum.START_TYPE_KEY));
    }

    @Test
    public void create_nonInitialActivity() {
        ActivityTracer trackableTracer = new ActivityTracer(mock(Activity.class), new AtomicReference<>("FirstActivity"), tracer, visibleScreenTracker, appStartupTimer);
        trackableTracer.startActivityCreation();
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Created", span.getName());
        assertNull(span.getAttributes().get(SplunkRum.START_TYPE_KEY));
    }

    @Test
    public void create_initialActivity() {
        ActivityTracer trackableTracer = new ActivityTracer(mock(Activity.class), new AtomicReference<>("Activity"), tracer, visibleScreenTracker, appStartupTimer);
        trackableTracer.startActivityCreation();
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("AppStart", span.getName());
        assertEquals("warm", span.getAttributes().get(SplunkRum.START_TYPE_KEY));
        assertEquals(SplunkRum.COMPONENT_APPSTART, span.getAttributes().get(SplunkRum.COMPONENT_KEY));
    }

    @Test
    public void create_initialActivity_firstTime() {
        appStartupTimer.start(tracer);
        ActivityTracer trackableTracer = new ActivityTracer(mock(Activity.class), new AtomicReference<>(), tracer, visibleScreenTracker, appStartupTimer);
        trackableTracer.startActivityCreation();
        trackableTracer.endActiveSpan();
        appStartupTimer.end();

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData appStartSpan = spans.get(0);
        assertEquals("AppStart", appStartSpan.getName());
        assertEquals("cold", appStartSpan.getAttributes().get(SplunkRum.START_TYPE_KEY));

        SpanData innerSpan = spans.get(1);
        assertEquals("Created", innerSpan.getName());
    }

    @Test
    public void addPreviousScreen_noPrevious() {
        VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);

        ActivityTracer trackableTracer = new ActivityTracer(mock(Activity.class), new AtomicReference<>(), tracer, visibleScreenTracker, appStartupTimer);

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));
    }

    @Test
    public void addPreviousScreen_currentSameAsPrevious() {
        VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("Activity");

        ActivityTracer trackableTracer = new ActivityTracer(mock(Activity.class), new AtomicReference<>(), tracer, visibleScreenTracker, appStartupTimer);

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));
    }

    @Test
    public void addPreviousScreen() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("previousScreen");

        ActivityTracer trackableTracer = new ActivityTracer(mock(Activity.class), new AtomicReference<>(), tracer, visibleScreenTracker, appStartupTimer);

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertEquals("previousScreen", span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));
    }

    @Test
    public void testAnnotatedActivity() {
        Activity annotatedActivity = new AnnotatedActivity();
        ActivityTracer activityTracer = new ActivityTracer(annotatedActivity, new AtomicReference<>(), tracer, visibleScreenTracker, appStartupTimer);
        activityTracer.startActivityCreation();
        activityTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("squarely", span.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
    }

    @RumScreenName("squarely")
    static class AnnotatedActivity extends Activity {
    }

    private SpanData getSingleSpan() {
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        return generatedSpans.get(0);
    }
}