package com.splunk.rum;

import androidx.fragment.app.Fragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class RumFragmentLifecycleCallbacksTest {
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void fragmentCreation() {
        FragmentCallbackTestHarness testHarness = new FragmentCallbackTestHarness(new RumFragmentLifecycleCallbacks(tracer));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentCreationLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals(fragment.getClass().getSimpleName() + " Created", spanData.getName());
        assertEquals(fragment.getClass().getSimpleName(), spanData.getAttributes().get(NamedTrackableTracer.FRAGMENT_NAME_KEY));
        assertEquals("ui", spanData.getAttributes().get(SplunkRum.COMPONENT_KEY));

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
    public void fragmentRestored() {
        FragmentCallbackTestHarness testHarness = new FragmentCallbackTestHarness(new RumFragmentLifecycleCallbacks(tracer));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentRestoredLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals(fragment.getClass().getSimpleName() + " Restored", spanData.getName());
        assertEquals(fragment.getClass().getSimpleName(), spanData.getAttributes().get(NamedTrackableTracer.FRAGMENT_NAME_KEY));
        assertEquals("ui", spanData.getAttributes().get(SplunkRum.COMPONENT_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(3, events.size());
        checkEventExists(events, "fragmentViewCreated");
        checkEventExists(events, "fragmentStarted");
        checkEventExists(events, "fragmentResumed");
    }

    @Test
    public void fragmentPaused() {
        FragmentCallbackTestHarness testHarness = new FragmentCallbackTestHarness(new RumFragmentLifecycleCallbacks(tracer));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentPausedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals(fragment.getClass().getSimpleName() + " Paused", spanData.getName());
        assertEquals(fragment.getClass().getSimpleName(), spanData.getAttributes().get(NamedTrackableTracer.FRAGMENT_NAME_KEY));
        assertEquals("ui", spanData.getAttributes().get(SplunkRum.COMPONENT_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentPaused");
        checkEventExists(events, "fragmentStopped");
    }

    @Test
    public void fragmentDetachedFromActive() {
        FragmentCallbackTestHarness testHarness = new FragmentCallbackTestHarness(new RumFragmentLifecycleCallbacks(tracer));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentDetachedFromActiveLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(3, spans.size());

        SpanData pauseSpan = spans.get(0);

        assertEquals(fragment.getClass().getSimpleName() + " Paused", pauseSpan.getName());
        assertEquals(fragment.getClass().getSimpleName(), pauseSpan.getAttributes().get(NamedTrackableTracer.FRAGMENT_NAME_KEY));
        assertEquals("ui", pauseSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));

        List<EventData> events = pauseSpan.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentPaused");
        checkEventExists(events, "fragmentStopped");

        SpanData destroyViewSpan = spans.get(1);

        assertEquals(fragment.getClass().getSimpleName() + " ViewDestroyed", destroyViewSpan.getName());
        assertNotNull(destroyViewSpan.getAttributes().get(NamedTrackableTracer.FRAGMENT_NAME_KEY));
        assertEquals("ui", destroyViewSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));

        events = destroyViewSpan.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentViewDestroyed");

        SpanData detachSpan = spans.get(2);

        assertEquals(fragment.getClass().getSimpleName() + " Destroyed", detachSpan.getName());
        assertNotNull(detachSpan.getAttributes().get(NamedTrackableTracer.FRAGMENT_NAME_KEY));
        assertEquals("ui", detachSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));

        events = detachSpan.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentDestroyed");
        checkEventExists(events, "fragmentDetached");
    }

    @Test
    public void fragmentDestroyedFromStopped() {
        FragmentCallbackTestHarness testHarness = new FragmentCallbackTestHarness(new RumFragmentLifecycleCallbacks(tracer));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentViewDestroyedFromStoppedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals(fragment.getClass().getSimpleName() + " ViewDestroyed", span.getName());
        assertNotNull(span.getAttributes().get(NamedTrackableTracer.FRAGMENT_NAME_KEY));
        assertEquals("ui", span.getAttributes().get(SplunkRum.COMPONENT_KEY));

        List<EventData> events = span.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentViewDestroyed");
    }

    @Test
    public void fragmentDetachedFromStopped() {
        FragmentCallbackTestHarness testHarness = new FragmentCallbackTestHarness(new RumFragmentLifecycleCallbacks(tracer));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentDetachedFromStoppedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData destroyViewSpan = spans.get(0);

        assertEquals(fragment.getClass().getSimpleName() + " ViewDestroyed", destroyViewSpan.getName());
        assertNotNull(destroyViewSpan.getAttributes().get(NamedTrackableTracer.FRAGMENT_NAME_KEY));
        assertEquals("ui", destroyViewSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        List<EventData> events = destroyViewSpan.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentViewDestroyed");

        SpanData detachSpan = spans.get(1);

        assertEquals(fragment.getClass().getSimpleName() + " Destroyed", detachSpan.getName());
        assertNotNull(detachSpan.getAttributes().get(NamedTrackableTracer.FRAGMENT_NAME_KEY));
        assertEquals("ui", detachSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        events = detachSpan.getEvents();
        assertEquals(2, events.size());
        checkEventExists(events, "fragmentDestroyed");
        checkEventExists(events, "fragmentDetached");
    }

    @Test
    public void fragmentDetached() {
        FragmentCallbackTestHarness testHarness = new FragmentCallbackTestHarness(new RumFragmentLifecycleCallbacks(tracer));

        Fragment fragment = mock(Fragment.class);
        testHarness.runFragmentDetachedLifecycle(fragment);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData detachSpan = spans.get(0);

        assertEquals(fragment.getClass().getSimpleName() + " Detached", detachSpan.getName());
        assertNotNull(detachSpan.getAttributes().get(NamedTrackableTracer.FRAGMENT_NAME_KEY));
        assertEquals("ui", detachSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));

        List<EventData> events = detachSpan.getEvents();
        assertEquals(1, events.size());
        checkEventExists(events, "fragmentDetached");
    }

    private void checkEventExists(List<EventData> events, String eventName) {
        Optional<EventData> event = events.stream().filter(e -> e.getName().equals(eventName)).findAny();
        assertTrue("Event with name " + eventName + " not found", event.isPresent());
    }
}