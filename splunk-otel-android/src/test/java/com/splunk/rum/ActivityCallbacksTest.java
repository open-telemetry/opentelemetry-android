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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static java.util.Collections.singletonList;

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

public class ActivityCallbacksTest {
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;
    private VisibleScreenTracker visibleScreenTracker;
    private final AppStartupTimer startupTimer = new AppStartupTimer();
    private final AppStateListener appStateListener = mock(AppStateListener.class);
    private final SlowRenderingDetector slowRenderingDetector = mock(SlowRenderingDetector.class);

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        visibleScreenTracker = mock(VisibleScreenTracker.class);
    }

    @Test
    public void appStartup() {
        startupTimer.start(tracer);
        ActivityCallbacks activityCallbacks = ActivityCallbacks.builder()
                .tracer(tracer)
                .visibleScreenTracker(visibleScreenTracker)
                .startupTimer(startupTimer)
                .appStateListeners(singletonList(appStateListener))
                .slowRenderingDetector(slowRenderingDetector)
                .build();
        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(activityCallbacks);

        Activity activity = mock(Activity.class);
        testHarness.runAppStartupLifecycle(activity);
        startupTimer.end();

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData startupSpan = spans.get(0);

        assertEquals("AppStart", startupSpan.getName());
        assertEquals("cold", startupSpan.getAttributes().get(SplunkRum.START_TYPE_KEY));

        SpanData creationSpan = spans.get(1);

        assertEquals(activity.getClass().getSimpleName(), creationSpan.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(activity.getClass().getSimpleName(), creationSpan.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_UI, creationSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(creationSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = creationSpan.getEvents();
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

        verify(appStateListener).appForegrounded();
        verify(slowRenderingDetector).add(activity);
    }

    @Test
    public void activityCreation() {
        ActivityCallbacks activityCallbacks = ActivityCallbacks.builder()
                .tracer(tracer)
                .visibleScreenTracker(visibleScreenTracker)
                .startupTimer(startupTimer)
                .appStateListeners(singletonList(appStateListener))
                .slowRenderingDetector(slowRenderingDetector)
                .build();

        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(activityCallbacks);
        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityCreationLifecycle(activity);
        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals("AppStart", span.getName());
        assertEquals("warm", span.getAttributes().get(SplunkRum.START_TYPE_KEY));
        assertEquals(activity.getClass().getSimpleName(), span.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(activity.getClass().getSimpleName(), span.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_APPSTART, span.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = span.getEvents();
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

        verify(appStateListener).appForegrounded();
    }

    private void startupAppAndClearSpans(ActivityCallbackTestHarness testHarness) {
        //make sure that the initial state has been set up & the application is started.
        testHarness.runAppStartupLifecycle(mock(Activity.class));
        otelTesting.clearSpans();
        verify(appStateListener).appForegrounded();
    }

    @Test
    public void activityRestart() {
        ActivityCallbacks activityCallbacks = ActivityCallbacks.builder()
                .tracer(tracer)
                .visibleScreenTracker(visibleScreenTracker)
                .startupTimer(startupTimer)
                .appStateListeners(singletonList(appStateListener))
                .slowRenderingDetector(slowRenderingDetector)
                .build();

        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(activityCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityRestartedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals("AppStart", span.getName());
        assertEquals("hot", span.getAttributes().get(SplunkRum.START_TYPE_KEY));
        assertEquals(activity.getClass().getSimpleName(), span.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(activity.getClass().getSimpleName(), span.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_APPSTART, span.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = span.getEvents();
        assertEquals(6, events.size());

        checkEventExists(events, "activityPreStarted");
        checkEventExists(events, "activityStarted");
        checkEventExists(events, "activityPostStarted");

        checkEventExists(events, "activityPreResumed");
        checkEventExists(events, "activityResumed");
        checkEventExists(events, "activityPostResumed");

        verify(appStateListener).appForegrounded();
    }

    @Test
    public void activityResumed() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("previousScreen");
        ActivityCallbacks activityCallbacks = ActivityCallbacks.builder()
                .tracer(tracer)
                .visibleScreenTracker(visibleScreenTracker)
                .startupTimer(startupTimer)
                .appStateListeners(singletonList(appStateListener))
                .slowRenderingDetector(slowRenderingDetector)
                .build();

        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(activityCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityResumedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals("Resumed", span.getName());
        assertEquals(activity.getClass().getSimpleName(), span.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(activity.getClass().getSimpleName(), span.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_UI, span.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertEquals("previousScreen", span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = span.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPreResumed");
        checkEventExists(events, "activityResumed");
        checkEventExists(events, "activityPostResumed");

        verifyNoMoreInteractions(appStateListener);
    }

    @Test
    public void activityDestroyedFromStopped() {
        ActivityCallbacks activityCallbacks = ActivityCallbacks.builder()
                .tracer(tracer)
                .visibleScreenTracker(visibleScreenTracker)
                .startupTimer(startupTimer)
                .appStateListeners(singletonList(appStateListener))
                .slowRenderingDetector(slowRenderingDetector)
                .build();

        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(activityCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityDestroyedFromStoppedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);

        assertEquals("Destroyed", span.getName());
        assertEquals(activity.getClass().getSimpleName(), span.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(activity.getClass().getSimpleName(), span.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_UI, span.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(span.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = span.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPreDestroyed");
        checkEventExists(events, "activityDestroyed");
        checkEventExists(events, "activityPostDestroyed");

        verify(appStateListener, never()).appBackgrounded();
    }

    @Test
    public void activityDestroyedFromPaused() {
        ActivityCallbacks activityCallbacks = ActivityCallbacks.builder()
                .tracer(tracer)
                .visibleScreenTracker(visibleScreenTracker)
                .startupTimer(startupTimer)
                .appStateListeners(singletonList(appStateListener))
                .slowRenderingDetector(slowRenderingDetector)
                .build();

        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(activityCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityDestroyedFromPausedLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData stoppedSpan = spans.get(0);

        assertEquals("Stopped", stoppedSpan.getName());
        assertEquals(activity.getClass().getSimpleName(), stoppedSpan.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(activity.getClass().getSimpleName(), stoppedSpan.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_UI, stoppedSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(stoppedSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = stoppedSpan.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPreStopped");
        checkEventExists(events, "activityStopped");
        checkEventExists(events, "activityPostStopped");

        SpanData destroyedSpan = spans.get(1);

        assertEquals("Destroyed", destroyedSpan.getName());
        assertEquals(activity.getClass().getSimpleName(), destroyedSpan.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(activity.getClass().getSimpleName(), destroyedSpan.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals("ui", destroyedSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(destroyedSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        events = destroyedSpan.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPreDestroyed");
        checkEventExists(events, "activityDestroyed");
        checkEventExists(events, "activityPostDestroyed");

        verify(appStateListener).appBackgrounded();
    }

    @Test
    public void activityStoppedFromRunning() {
        ActivityCallbacks activityCallbacks = ActivityCallbacks.builder()
                .tracer(tracer)
                .visibleScreenTracker(visibleScreenTracker)
                .startupTimer(startupTimer)
                .appStateListeners(singletonList(appStateListener))
                .slowRenderingDetector(slowRenderingDetector)
                .build();

        ActivityCallbackTestHarness testHarness = new ActivityCallbackTestHarness(activityCallbacks);

        startupAppAndClearSpans(testHarness);

        Activity activity = mock(Activity.class);
        testHarness.runActivityStoppedFromRunningLifecycle(activity);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData stoppedSpan = spans.get(0);

        assertEquals("Paused", stoppedSpan.getName());
        assertEquals(activity.getClass().getSimpleName(), stoppedSpan.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(activity.getClass().getSimpleName(), stoppedSpan.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals(SplunkRum.COMPONENT_UI, stoppedSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(stoppedSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        List<EventData> events = stoppedSpan.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPrePaused");
        checkEventExists(events, "activityPaused");
        checkEventExists(events, "activityPostPaused");
        verify(slowRenderingDetector).stop(activity);

        SpanData destroyedSpan = spans.get(1);

        assertEquals("Stopped", destroyedSpan.getName());
        assertEquals(activity.getClass().getSimpleName(), destroyedSpan.getAttributes().get(ActivityTracer.ACTIVITY_NAME_KEY));
        assertEquals(activity.getClass().getSimpleName(), destroyedSpan.getAttributes().get(SplunkRum.SCREEN_NAME_KEY));
        assertEquals("ui", destroyedSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertNull(destroyedSpan.getAttributes().get(SplunkRum.LAST_SCREEN_NAME_KEY));

        events = destroyedSpan.getEvents();
        assertEquals(3, events.size());

        checkEventExists(events, "activityPreStopped");
        checkEventExists(events, "activityStopped");
        checkEventExists(events, "activityPostStopped");

        verify(appStateListener).appBackgrounded();
    }

    private void checkEventExists(List<EventData> events, String eventName) {
        Optional<EventData> event = events.stream().filter(e -> e.getName().equals(eventName)).findAny();
        assertTrue("Event with name " + eventName + " not found", event.isPresent());
    }
}