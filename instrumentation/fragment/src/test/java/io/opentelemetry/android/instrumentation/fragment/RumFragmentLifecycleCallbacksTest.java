/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment;

import static io.opentelemetry.android.common.RumConstants.LAST_SCREEN_NAME_KEY;
import static io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RumFragmentLifecycleCallbacksTest {
    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();
    private final VisibleScreenTracker visibleScreenTracker =
            Mockito.mock(VisibleScreenTracker.class);
    private Tracer tracer;
    @Mock private ScreenNameExtractor screenNameExtractor;

    @BeforeEach
    void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        when(screenNameExtractor.extract(isA(Fragment.class))).thenReturn("Fragment");
    }

    @Test
    void fragmentCreation() {
        FragmentCallbackTestHarness testHarness = getFragmentCallbackTestHarness();

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
        FragmentCallbackTestHarness testHarness = getFragmentCallbackTestHarness();

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
        FragmentCallbackTestHarness testHarness = getFragmentCallbackTestHarness();

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
        FragmentCallbackTestHarness testHarness = getFragmentCallbackTestHarness();

        Fragment fragment = mock(Fragment.class);
        // calls onFragmentPaused() and onFragmentStopped()
        testHarness.runFragmentPausedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        // one paused, one stopped
        assertEquals(2, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals("Paused", spanData.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                spanData.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(), spanData.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(spanData.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentPaused");

        SpanData stopSpan = spans.get(1);

        assertEquals("Stopped", stopSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                stopSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(), stopSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(stopSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> stopEvents = stopSpan.getEvents();
        assertEquals(1, stopEvents.size());
        checkEventExists(stopEvents, "fragmentStopped");
    }

    @Test
    void fragmentDetachedFromActive() {
        FragmentCallbackTestHarness testHarness = getFragmentCallbackTestHarness();

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentDetachedFromActiveLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();

        assertEquals(4, spans.size());

        SpanData pauseSpan = spans.get(0);
        SpanData stopSpan = spans.get(1);

        assertEquals("Paused", pauseSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                pauseSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(),
                pauseSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(pauseSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = pauseSpan.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentPaused");

        assertEquals("Stopped", stopSpan.getName());
        assertEquals(
                fragment.getClass().getSimpleName(),
                stopSpan.getAttributes().get(FragmentTracer.FRAGMENT_NAME_KEY));
        assertEquals(
                fragment.getClass().getSimpleName(), stopSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(stopSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> stopEvents = stopSpan.getEvents();
        assertEquals(1, stopEvents.size());
        checkEventExists(stopEvents, "fragmentStopped");

        SpanData destroyViewSpan = spans.get(2);

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

        SpanData detachSpan = spans.get(3);

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
        FragmentCallbackTestHarness testHarness = getFragmentCallbackTestHarness();

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
        FragmentCallbackTestHarness testHarness = getFragmentCallbackTestHarness();

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
        FragmentCallbackTestHarness testHarness = getFragmentCallbackTestHarness();

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

    private @NonNull FragmentCallbackTestHarness getFragmentCallbackTestHarness() {
        return new FragmentCallbackTestHarness(
                new RumFragmentLifecycleCallbacks(
                        tracer,
                        visibleScreenTracker::getPreviouslyVisibleScreen,
                        screenNameExtractor));
    }
}
