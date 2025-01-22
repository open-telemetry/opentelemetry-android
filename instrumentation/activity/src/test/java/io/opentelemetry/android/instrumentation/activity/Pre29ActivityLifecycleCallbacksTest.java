/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity;

import static io.opentelemetry.android.common.RumConstants.LAST_SCREEN_NAME_KEY;
import static io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY;
import static io.opentelemetry.android.common.RumConstants.START_TYPE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer;
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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

class Pre29ActivityLifecycleCallbacksTest {
    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();
    private ActivityTracerCache tracers;

    private VisibleScreenTracker visibleScreenTracker;

    @BeforeEach
    void setup() {
        AppStartupTimer appStartupTimer = new AppStartupTimer();
        Tracer tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        visibleScreenTracker = Mockito.mock(VisibleScreenTracker.class);
        ScreenNameExtractor extractor = mock(ScreenNameExtractor.class);
        when(extractor.extract(isA(Activity.class))).thenReturn("Activity");
        tracers = new ActivityTracerCache(tracer, visibleScreenTracker, appStartupTimer, extractor);
    }

    @Test
    void appStartup() {
        Pre29ActivityCallbacks rumLifecycleCallbacks = new Pre29ActivityCallbacks(tracers);
        Pre29ActivityCallbackTestHarness testHarness =
                new Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks);

        Activity activity = mock(Activity.class);
        testHarness.runAppStartupLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData creationSpan = spans.get(0);

        // TODO: Add test to relevant components
        //        assertEquals("AppStart", appStartSpan.getName());
        //        assertEquals("cold", appStartSpan.getAttributes().get(SplunkRum.START_TYPE_KEY));

        assertEquals(
                activity.getClass().getSimpleName(),
                creationSpan.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(
                activity.getClass().getSimpleName(),
                creationSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(creationSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = creationSpan.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityCreated");
        checkEventExists(events, "activityStarted");
        checkEventExists(events, "activityResumed");
    }

    @Test
    void activityCreation() {
        Pre29ActivityCallbacks rumLifecycleCallbacks = new Pre29ActivityCallbacks(tracers);
        Pre29ActivityCallbackTestHarness testHarness =
                new Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks);
        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityCreationLifecycle(activity);
        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals("AppStart", span.getName());
        assertEquals("warm", span.getAttributes().get(START_TYPE_KEY));
        assertEquals(
                activity.getClass().getSimpleName(),
                span.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(
                activity.getClass().getSimpleName(), span.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(span.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = span.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityCreated");
        checkEventExists(events, "activityStarted");
        checkEventExists(events, "activityResumed");
    }

    private void startupAppAndClearSpans(Pre29ActivityCallbackTestHarness testHarness) {
        // make sure that the initial state has been set up & the application is started.
        testHarness.runAppStartupLifecycle(mock(Activity.class));
        otelTesting.clearSpans();
    }

    @Test
    void activityRestart() {
        Pre29ActivityCallbacks rumLifecycleCallbacks = new Pre29ActivityCallbacks(tracers);
        Pre29ActivityCallbackTestHarness testHarness =
                new Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityRestartedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals("AppStart", span.getName());
        assertEquals("hot", span.getAttributes().get(START_TYPE_KEY));
        assertEquals(
                activity.getClass().getSimpleName(),
                span.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(
                activity.getClass().getSimpleName(), span.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(span.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = span.getEvents();
        assertEquals(2, events.size());

        checkEventExists(events, "activityStarted");
        checkEventExists(events, "activityResumed");
    }

    @Test
    void activityResumed() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("previousScreen");

        Pre29ActivityCallbacks rumLifecycleCallbacks = new Pre29ActivityCallbacks(tracers);
        Pre29ActivityCallbackTestHarness testHarness =
                new Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityResumedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals("Resumed", span.getName());
        assertEquals(
                activity.getClass().getSimpleName(),
                span.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(
                activity.getClass().getSimpleName(), span.getAttributes().get(SCREEN_NAME_KEY));
        assertEquals("previousScreen", span.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = span.getEvents();
        assertEquals(1, events.size());

        checkEventExists(events, "activityResumed");
    }

    @Test
    void activityDestroyedFromStopped() {
        Pre29ActivityCallbacks rumLifecycleCallbacks = new Pre29ActivityCallbacks(tracers);
        Pre29ActivityCallbackTestHarness testHarness =
                new Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityDestroyedFromStoppedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals("Destroyed", span.getName());
        assertEquals(
                activity.getClass().getSimpleName(),
                span.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(
                activity.getClass().getSimpleName(), span.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(span.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = span.getEvents();
        assertEquals(1, events.size());

        checkEventExists(events, "activityDestroyed");
    }

    @Test
    void activityDestroyedFromPaused() {
        Pre29ActivityCallbacks rumLifecycleCallbacks = new Pre29ActivityCallbacks(tracers);
        Pre29ActivityCallbackTestHarness testHarness =
                new Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityDestroyedFromPausedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData stoppedSpan = spans.get(0);

        assertEquals("Stopped", stoppedSpan.getName());
        assertEquals(
                activity.getClass().getSimpleName(),
                stoppedSpan.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(
                activity.getClass().getSimpleName(),
                stoppedSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(stoppedSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = stoppedSpan.getEvents();
        assertEquals(1, events.size());

        checkEventExists(events, "activityStopped");

        SpanData destroyedSpan = spans.get(1);

        assertEquals("Destroyed", destroyedSpan.getName());
        assertEquals(
                activity.getClass().getSimpleName(),
                destroyedSpan.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(
                activity.getClass().getSimpleName(),
                destroyedSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(destroyedSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        events = destroyedSpan.getEvents();
        assertEquals(1, events.size());

        checkEventExists(events, "activityDestroyed");
    }

    @Test
    void activityStoppedFromRunning() {
        Pre29ActivityCallbacks rumLifecycleCallbacks = new Pre29ActivityCallbacks(tracers);
        Pre29ActivityCallbackTestHarness testHarness =
                new Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityStoppedFromRunningLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData stoppedSpan = spans.get(0);

        assertEquals("Paused", stoppedSpan.getName());
        assertEquals(
                activity.getClass().getSimpleName(),
                stoppedSpan.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(
                activity.getClass().getSimpleName(),
                stoppedSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(stoppedSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        List<EventData> events = stoppedSpan.getEvents();
        assertEquals(1, events.size());

        checkEventExists(events, "activityPaused");

        SpanData destroyedSpan = spans.get(1);

        assertEquals("Stopped", destroyedSpan.getName());
        assertEquals(
                activity.getClass().getSimpleName(),
                destroyedSpan.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(
                activity.getClass().getSimpleName(),
                destroyedSpan.getAttributes().get(SCREEN_NAME_KEY));
        assertNull(destroyedSpan.getAttributes().get(LAST_SCREEN_NAME_KEY));

        events = destroyedSpan.getEvents();
        assertEquals(1, events.size());

        checkEventExists(events, "activityStopped");
    }

    private void checkEventExists(List<EventData> events, String eventName) {
        Optional<EventData> event =
                events.stream().filter(e -> e.getName().equals(eventName)).findAny();
        assertTrue(event.isPresent(), "Event with name " + eventName + " not found");
    }
}
