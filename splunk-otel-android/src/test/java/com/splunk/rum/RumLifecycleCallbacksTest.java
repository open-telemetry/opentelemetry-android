package com.splunk.rum;

import android.app.Activity;

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

public class RumLifecycleCallbacksTest {
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void appStartup() {
        RumLifecycleCallbacks rumLifecycleCallbacks = new RumLifecycleCallbacks(tracer);
        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(rumLifecycleCallbacks);

        testHarness.runAppStartupLifecycle(mock(Activity.class));

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals("App Startup", spanData.getName());
        assertNotNull(spanData.getAttributes().get(NamedTrackableTracer.ACTIVITY_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(9, events.size());

        checkEventExists(events, "activityPreCreated");
        checkEventExists(events, "activityCreated");
        checkEventExists(events, "activityPostCreated");

        checkEventExists(events, "activityPreStarted");
        checkEventExists(events, "activityStarted");
        checkEventExists(events, "activityPostStarted");

        checkEventExists(events, "activityPreResumed");
        checkEventExists(events, "activityResumed");
        checkEventExists(events, "activityPostResumed");
    }

    @Test
    public void activityCreation() {
        RumLifecycleCallbacks rumLifecycleCallbacks = new RumLifecycleCallbacks(tracer);
        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(rumLifecycleCallbacks);
        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityCreationLifecycle(activity);
        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals(activity.getClass().getSimpleName() + " Created", spanData.getName());
        assertNotNull(spanData.getAttributes().get(NamedTrackableTracer.ACTIVITY_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(9, events.size());

        checkEventExists(events, "activityPreCreated");
        checkEventExists(events, "activityCreated");
        checkEventExists(events, "activityPostCreated");

        checkEventExists(events, "activityPreStarted");
        checkEventExists(events, "activityStarted");
        checkEventExists(events, "activityPostStarted");

        checkEventExists(events, "activityPreResumed");
        checkEventExists(events, "activityResumed");
        checkEventExists(events, "activityPostResumed");
    }

    private void startupAppAndClearSpans(ActivityCallbackTestHarness testHarness) {
        //make sure that the initial state has been set up & the application is started.
        testHarness.runAppStartupLifecycle(mock(Activity.class));
        otelTesting.clearSpans();
    }

    @Test
    public void activityRestart() {
        RumLifecycleCallbacks rumLifecycleCallbacks = new RumLifecycleCallbacks(tracer);
        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(rumLifecycleCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityRestartedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals(activity.getClass().getSimpleName() + " Restarted", spanData.getName());
        assertNotNull(spanData.getAttributes().get(NamedTrackableTracer.ACTIVITY_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(6, events.size());

        checkEventExists(events, "activityPreStarted");
        checkEventExists(events, "activityStarted");
        checkEventExists(events, "activityPostStarted");

        checkEventExists(events, "activityPreResumed");
        checkEventExists(events, "activityResumed");
        checkEventExists(events, "activityPostResumed");
    }

    @Test
    public void activityResumed() {
        RumLifecycleCallbacks rumLifecycleCallbacks = new RumLifecycleCallbacks(tracer);
        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(rumLifecycleCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityResumedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals(activity.getClass().getSimpleName() + " Resumed", spanData.getName());
        assertNotNull(spanData.getAttributes().get(NamedTrackableTracer.ACTIVITY_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPreResumed");
        checkEventExists(events, "activityResumed");
        checkEventExists(events, "activityPostResumed");
    }

    @Test
    public void activityDestroyedFromStopped() {
        RumLifecycleCallbacks rumLifecycleCallbacks = new RumLifecycleCallbacks(tracer);
        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(rumLifecycleCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityDestroyedFromStoppedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals(activity.getClass().getSimpleName() + " Destroyed", spanData.getName());
        assertNotNull(spanData.getAttributes().get(NamedTrackableTracer.ACTIVITY_NAME_KEY));

        List<EventData> events = spanData.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPreDestroyed");
        checkEventExists(events, "activityDestroyed");
        checkEventExists(events, "activityPostDestroyed");
    }

    @Test
    public void activityDestroyedFromPaused() {
        RumLifecycleCallbacks rumLifecycleCallbacks = new RumLifecycleCallbacks(tracer);
        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(rumLifecycleCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityDestroyedFromPausedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData stoppedSpan = spans.get(0);

        assertEquals(activity.getClass().getSimpleName() + " Stopped", stoppedSpan.getName());
        assertNotNull(stoppedSpan.getAttributes().get(NamedTrackableTracer.ACTIVITY_NAME_KEY));

        List<EventData> events = stoppedSpan.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPreStopped");
        checkEventExists(events, "activityStopped");
        checkEventExists(events, "activityPostStopped");

        SpanData destroyedSpan = spans.get(1);

        assertEquals(activity.getClass().getSimpleName() + " Destroyed", destroyedSpan.getName());
        assertNotNull(destroyedSpan.getAttributes().get(NamedTrackableTracer.ACTIVITY_NAME_KEY));

        events = destroyedSpan.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPreDestroyed");
        checkEventExists(events, "activityDestroyed");
        checkEventExists(events, "activityPostDestroyed");
    }

    @Test
    public void activityStoppedFromRunning() {
        RumLifecycleCallbacks rumLifecycleCallbacks = new RumLifecycleCallbacks(tracer);
        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(rumLifecycleCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityStoppedFromRunningLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData stoppedSpan = spans.get(0);

        assertEquals(activity.getClass().getSimpleName() + " Paused", stoppedSpan.getName());
        assertNotNull(stoppedSpan.getAttributes().get(NamedTrackableTracer.ACTIVITY_NAME_KEY));

        List<EventData> events = stoppedSpan.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPrePaused");
        checkEventExists(events, "activityPaused");
        checkEventExists(events, "activityPostPaused");

        SpanData destroyedSpan = spans.get(1);

        assertEquals(activity.getClass().getSimpleName() + " Stopped", destroyedSpan.getName());
        assertNotNull(destroyedSpan.getAttributes().get(NamedTrackableTracer.ACTIVITY_NAME_KEY));

        events = destroyedSpan.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPreStopped");
        checkEventExists(events, "activityStopped");
        checkEventExists(events, "activityPostStopped");
    }

    private void checkEventExists(List<EventData> events, String eventName) {
        Optional<EventData> event = events.stream().filter(e -> e.getName().equals(eventName)).findAny();
        assertTrue("Event with name " + eventName + " not found", event.isPresent());
    }
}