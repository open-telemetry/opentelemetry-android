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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.fragment.app.Fragment;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RumFragmentLifecycleCallbacksTest {
    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();
    private Tracer tracer;
    private final VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);

    @BeforeEach
    void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    void fragmentCreation() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentCreationLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals("Created", spanData.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                spanData.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                spanData.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_UI, spanData.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(spanData.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(7, events.size());
        checkEventExists(events, "fragmentPreAttached");
        checkEventExists(events, "fragmentAttached");
        checkEventExists(events, "fragmentPreCreated");
        checkEventExists(events, "fragmentCreated");
        checkEventExists(events, "fragmentViewCreated");
        checkEventExists(events, "fragmentStarted");
        checkEventExists(events, "fragmentResumed");
    }

    @Test
    void fragmentRestored() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("previousScreen");
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentRestoredLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals("Restored", spanData.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                spanData.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                spanData.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_UI, spanData.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertEquals(
                "previousScreen", spanData.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(3, events.size());
        checkEventExists(events, "fragmentViewCreated");
        checkEventExists(events, "fragmentStarted");
        checkEventExists(events, "fragmentResumed");
    }

    @Test
    void fragmentResumed() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentResumedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals("Resumed", spanData.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                spanData.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_UI, spanData.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(spanData.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentResumed");
    }

    @Test
    void fragmentPaused() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentPausedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals("Paused", spanData.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                spanData.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                spanData.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_UI, spanData.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(spanData.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentPaused");
        checkEventExists(events, "fragmentStopped");
    }

    @Test
    void fragmentDetachedFromActive() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentDetachedFromActiveLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(3, spans.size());

        SpanData pauseSpan = spans.get(0);

        assertEquals("Paused", pauseSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                pauseSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                pauseSpan.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(
                SplunkRum.COMPONENT_UI, pauseSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(pauseSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = pauseSpan.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentPaused");
        checkEventExists(events, "fragmentStopped");

        SpanData destroyViewSpan = spans.get(1);

        assertEquals("ViewDestroyed", destroyViewSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                destroyViewSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                destroyViewSpan.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(
                SplunkRum.COMPONENT_UI,
                destroyViewSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(destroyViewSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        events = destroyViewSpan.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentViewDestroyed");

        SpanData detachSpan = spans.get(2);

        assertEquals("Destroyed", detachSpan.getName());
        assertNotNull(detachSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                SplunkRum.COMPONENT_UI, detachSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(detachSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        events = detachSpan.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentDestroyed");
        checkEventExists(events, "fragmentDetached");
    }

    @Test
    void fragmentDestroyedFromStopped() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentViewDestroyedFromStoppedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals("ViewDestroyed", span.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                span.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                span.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_UI, span.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = span.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentViewDestroyed");
    }

    @Test
    void fragmentDetachedFromStopped() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentDetachedFromStoppedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData destroyViewSpan = spans.get(0);

        assertEquals("ViewDestroyed", destroyViewSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                destroyViewSpan.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                destroyViewSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                SplunkRum.COMPONENT_UI,
                destroyViewSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(destroyViewSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = destroyViewSpan.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentViewDestroyed");

        SpanData detachSpan = spans.get(1);

        assertEquals("Destroyed", detachSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                detachSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                SplunkRum.COMPONENT_UI, detachSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(detachSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        events = detachSpan.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentDestroyed");
        checkEventExists(events, "fragmentDetached");
    }

    @Test
    void fragmentDetached() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentDetachedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData detachSpan = spans.get(0);

        assertEquals("Detached", detachSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                detachSpan.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                detachSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                SplunkRum.COMPONENT_UI, detachSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(detachSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = detachSpan.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentDetached");
    }

    private void checkEventExists(List<EventData> events, String eventName) {
        Optional<EventData> event =
                events.stream().filter(e -> e.getName().equals(eventName)).findAny();
        assertTrue(event.isPresent(), "Event with name " + eventName + " not found");
    }
}
