/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment;

import static io.opentelemetry.android.RumConstants.LAST_SCREEN_NAME_KEY;
import static io.opentelemetry.android.RumConstants.SCREEN_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.fragment.app.Fragment;
import io.opentelemetry.android.instrumentation.ScreenNameExtractor;
import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RumFragmentLifecycleCallbacksTest {
    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();
    private final VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);
    private Tracer tracer;
    @Mock private ScreenNameExtractor screenNameExtractor;

    @BeforeEach
    void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        when(screenNameExtractor.extract(isA(Fragment.class))).thenReturn("Fragment");
    }

    @Test
    void fragmentCreation() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(
                                tracer, visibleScreenTracker, screenNameExtractor));

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
                fragment.getClass().getSimpleName(), spanData.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(spanData.getAttributes().get(LAST_SCREEN_NAME_KEY));

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
                        new RumFragmentLifecycleCallbacks(
                                tracer, visibleScreenTracker, screenNameExtractor));

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
                fragment.getClass().getSimpleName(), spanData.getAttributes().get(SCREEN_NAME_KEY));
        assertEquals("previousScreen", spanData.getAttributes().get(LAST_SCREEN_NAME_KEY));

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
                        new RumFragmentLifecycleCallbacks(
                                tracer, visibleScreenTracker, screenNameExtractor));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentResumedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals("Resumed", spanData.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                spanData.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertNull(spanData.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentResumed");
    }

    @Test
    void fragmentPaused() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(
                                tracer, visibleScreenTracker, screenNameExtractor));

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
                fragment.getClass().getSimpleName(), spanData.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(spanData.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentPaused");
        checkEventExists(events, "fragmentStopped");
    }

    @Test
    void fragmentDetachedFromActive() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(
                                tracer, visibleScreenTracker, screenNameExtractor));

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
                pauseSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(pauseSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

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
                destroyViewSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(destroyViewSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        events = destroyViewSpan.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentViewDestroyed");

        SpanData detachSpan = spans.get(2);

        assertEquals("Destroyed", detachSpan.getName());
        assertNotNull(detachSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertNull(detachSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        events = detachSpan.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentDestroyed");
        checkEventExists(events, "fragmentDetached");
    }

    @Test
    void fragmentDestroyedFromStopped() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(
                                tracer, visibleScreenTracker, screenNameExtractor));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentViewDestroyedFromStoppedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals("ViewDestroyed", span.getName());
        assertEquals(
                fragment.getClass().getSimpleName(), span.getAttributes().get(SCREEN_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                span.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertNull(span.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = span.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentViewDestroyed");
    }

    @Test
    void fragmentDetachedFromStopped() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(
                                tracer, visibleScreenTracker, screenNameExtractor));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentDetachedFromStoppedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData destroyViewSpan = spans.get(0);

        assertEquals("ViewDestroyed", destroyViewSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                destroyViewSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                destroyViewSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertNull(destroyViewSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = destroyViewSpan.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentViewDestroyed");

        SpanData detachSpan = spans.get(1);

        assertEquals("Destroyed", detachSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                detachSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertNull(detachSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        events = detachSpan.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentDestroyed");
        checkEventExists(events, "fragmentDetached");
    }

    @Test
    void fragmentDetached() {
        FragmentCallbackTestHarness testHarness =
                new FragmentCallbackTestHarness(
                        new RumFragmentLifecycleCallbacks(
                                tracer, visibleScreenTracker, screenNameExtractor));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentDetachedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData detachSpan = spans.get(0);

        assertEquals("Detached", detachSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                detachSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                detachSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertNull(detachSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

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
