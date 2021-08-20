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

import androidx.fragment.app.Fragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;

public class FragmentTracerTest {
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;
    private final VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void create() {
        FragmentTracer trackableTracer = new FragmentTracer(mock(Fragment.class), tracer, visibleScreenTracker);
        trackableTracer.startFragmentCreation();
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Created", span.getName());
    }

    @Test
    public void addPreviousScreen_noPrevious() {
        VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);

        FragmentTracer trackableTracer = new FragmentTracer(mock(Fragment.class), tracer, visibleScreenTracker);

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));
    }

    @Test
    public void addPreviousScreen_currentSameAsPrevious() {
        VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("Fragment");

        FragmentTracer trackableTracer = new FragmentTracer(mock(Fragment.class), tracer, visibleScreenTracker);

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));
    }

    @Test
    public void addPreviousScreen() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("previousScreen");

        FragmentTracer fragmentTracer = new FragmentTracer(mock(Fragment.class), tracer, visibleScreenTracker);

        fragmentTracer.startSpanIfNoneInProgress("starting");
        fragmentTracer.addPreviousScreenAttribute();
        fragmentTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertEquals("previousScreen", span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));
    }

    private SpanData getSingleSpan() {
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        return generatedSpans.get(0);
    }
}