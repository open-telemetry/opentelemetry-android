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
        CallbackTestHarness testHarness = new CallbackTestHarness(rumLifecycleCallbacks);

        testHarness.runAppStartupLifecycle(mock(Activity.class));

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData spanData = spans.get(0);

        assertEquals("App Startup", spanData.getName());
        assertNotNull(spanData.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));

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

    private void checkEventExists(List<EventData> events, String eventName) {
        Optional<EventData> event = events.stream().filter(e -> e.getName().equals(eventName)).findAny();
        assertTrue("Event with name " + eventName + " not found", event.isPresent());
    }
}