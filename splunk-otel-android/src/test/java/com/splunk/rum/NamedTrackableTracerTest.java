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

import android.app.Activity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class NamedTrackableTracerTest {
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void restart_nonInitialActivity() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>("FirstActivity"), tracer);
        trackableTracer.initiateRestartSpanIfNecessary(false);
        trackableTracer.endActiveSpan();
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        SpanData span = generatedSpans.get(0);
        assertEquals("Activity Restarted", span.getName());
        assertNull(span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void restart_initialActivity() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>("Activity"), tracer);
        trackableTracer.initiateRestartSpanIfNecessary(false);
        trackableTracer.endActiveSpan();
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        SpanData span = generatedSpans.get(0);
        assertEquals("AppStart", span.getName());
        assertEquals("hot", span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void restart_initialActivity_multiActivityApp() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>("Activity"), tracer);
        trackableTracer.initiateRestartSpanIfNecessary(true);
        trackableTracer.endActiveSpan();
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        SpanData span = generatedSpans.get(0);
        assertEquals("Activity Restarted", span.getName());
        assertNull(span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void create_nonInitialActivity() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>("FirstActivity"), tracer);
        trackableTracer.startTrackableCreation();
        trackableTracer.endActiveSpan();
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        SpanData span = generatedSpans.get(0);
        assertEquals("Activity Created", span.getName());
        assertNull(span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void create_initialActivity() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>("Activity"), tracer);
        trackableTracer.startTrackableCreation();
        trackableTracer.endActiveSpan();
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        SpanData span = generatedSpans.get(0);
        assertEquals("AppStart", span.getName());
        assertEquals("warm", span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }

    @Test
    public void create_initialActivity_firstTime() {
        NamedTrackableTracer trackableTracer = new NamedTrackableTracer(mock(Activity.class), new AtomicReference<>(), tracer);
        trackableTracer.startTrackableCreation();
        trackableTracer.endActiveSpan();
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        SpanData span = generatedSpans.get(0);
        assertEquals("AppStart", span.getName());
        assertEquals("cold", span.getAttributes().get(NamedTrackableTracer.START_TYPE_KEY));
    }
}