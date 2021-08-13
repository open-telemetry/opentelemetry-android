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

public class NamedTrackableTracerTest {
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;
    private final VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void restart_nonInitialActivity() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>("FirstActivity"), tracer, visibleScreenTracker);
        trackableTracer.initiateRestartSpanIfNecessary(false);
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Restarted", span.getName());
        assertNull(span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void restart_initialActivity() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>("Activity"), tracer, visibleScreenTracker);
        trackableTracer.initiateRestartSpanIfNecessary(false);
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("AppStart", span.getName());
        assertEquals("hot", span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void restart_initialActivity_multiActivityApp() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>("Activity"), tracer, visibleScreenTracker);
        trackableTracer.initiateRestartSpanIfNecessary(true);
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Restarted", span.getName());
        assertNull(span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void create_nonInitialActivity() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>("FirstActivity"), tracer, visibleScreenTracker);
        trackableTracer.startTrackableCreation();
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Created", span.getName());
        assertNull(span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void create_initialActivity() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>("Activity"), tracer, visibleScreenTracker);
        trackableTracer.startTrackableCreation();
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("AppStart", span.getName());
        assertEquals("warm", span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void create_initialActivity_firstTime() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>(), tracer, visibleScreenTracker);
        trackableTracer.startTrackableCreation();
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("AppStart", span.getName());
        assertEquals("cold", span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void addPreviousScreen_noPrevious() {
        VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);

        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>(), tracer, visibleScreenTracker);

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

        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>(), tracer, visibleScreenTracker);

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));
    }

    @Test
    public void addPreviousScreen() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("previousScreen");

        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>(), tracer, visibleScreenTracker);

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertEquals("previousScreen", span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));
    }

    private SpanData getSingleSpan() {
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        return generatedSpans.get(0);
    }
}