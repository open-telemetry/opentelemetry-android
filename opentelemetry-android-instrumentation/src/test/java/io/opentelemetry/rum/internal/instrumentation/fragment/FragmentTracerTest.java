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

package io.opentelemetry.rum.internal.instrumentation.fragment;

import static io.opentelemetry.rum.internal.RumConstants.LAST_SCREEN_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.fragment.app.Fragment;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.rum.internal.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.rum.internal.util.ActiveSpan;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class FragmentTracerTest {
    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();
    private Tracer tracer;
    private ActiveSpan activeSpan;

    @BeforeEach
    void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);
        activeSpan = new ActiveSpan(visibleScreenTracker::getPreviouslyVisibleScreen);
    }

    @Test
    void create() {
        FragmentTracer trackableTracer =
                FragmentTracer.builder(mock(Fragment.class))
                        .setTracer(tracer)
                        .setActiveSpan(activeSpan)
                        .build();
        trackableTracer.startFragmentCreation();
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Created", span.getName());
    }

    @Test
    void addPreviousScreen_noPrevious() {
        VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);

        FragmentTracer trackableTracer =
                FragmentTracer.builder(mock(Fragment.class))
                        .setTracer(tracer)
                        .setActiveSpan(activeSpan)
                        .build();

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(LAST_SCREEN_NAME_KEY));
    }

    @Test
    void addPreviousScreen_currentSameAsPrevious() {
        VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("Fragment");

        FragmentTracer trackableTracer =
                FragmentTracer.builder(mock(Fragment.class))
                        .setTracer(tracer)
                        .setActiveSpan(activeSpan)
                        .build();

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(LAST_SCREEN_NAME_KEY));
    }

    @Test
    void addPreviousScreen() {

        VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("previousScreen");
        activeSpan = new ActiveSpan(visibleScreenTracker::getPreviouslyVisibleScreen);

        FragmentTracer fragmentTracer =
                FragmentTracer.builder(mock(Fragment.class))
                        .setTracer(tracer)
                        .setActiveSpan(activeSpan)
                        .build();

        fragmentTracer.startSpanIfNoneInProgress("starting");
        fragmentTracer.addPreviousScreenAttribute();
        fragmentTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertEquals("previousScreen", span.getAttributes().get(LAST_SCREEN_NAME_KEY));
    }

    private SpanData getSingleSpan() {
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        return generatedSpans.get(0);
    }
}
