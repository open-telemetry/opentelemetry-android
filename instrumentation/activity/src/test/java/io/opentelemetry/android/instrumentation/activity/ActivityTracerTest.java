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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer;
import io.opentelemetry.android.instrumentation.common.ActiveSpan;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

public class ActivityTracerTest {
    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    private Tracer tracer;
    private final VisibleScreenTracker visibleScreenTracker =
            Mockito.mock(VisibleScreenTracker.class);
    private final AppStartupTimer appStartupTimer = new AppStartupTimer();
    private ActiveSpan activeSpan;

    @BeforeEach
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        activeSpan = new ActiveSpan(visibleScreenTracker::getPreviouslyVisibleScreen);
    }

    @Test
    void restart_nonInitialActivity() {
        ActivityTracer trackableTracer =
                ActivityTracer.builder(mock(Activity.class))
                        .setInitialAppActivity("FirstActivity")
                        .setTracer(tracer)
                        .setAppStartupTimer(appStartupTimer)
                        .setActiveSpan(activeSpan)
                        .build();
        trackableTracer.initiateRestartSpanIfNecessary(false);
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Restarted", span.getName());
        assertNull(span.getAttributes().get(START_TYPE_KEY));
    }

    @Test
    public void restart_initialActivity() {
        ActivityTracer trackableTracer =
                ActivityTracer.builder(mock(Activity.class))
                        .setInitialAppActivity("Activity")
                        .setTracer(tracer)
                        .setAppStartupTimer(appStartupTimer)
                        .setActiveSpan(activeSpan)
                        .build();
        trackableTracer.initiateRestartSpanIfNecessary(false);
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("AppStart", span.getName());
        assertEquals("hot", span.getAttributes().get(START_TYPE_KEY));
    }

    @Test
    public void restart_initialActivity_multiActivityApp() {
        ActivityTracer trackableTracer =
                ActivityTracer.builder(mock(Activity.class))
                        .setInitialAppActivity("Activity")
                        .setTracer(tracer)
                        .setAppStartupTimer(appStartupTimer)
                        .setActiveSpan(activeSpan)
                        .build();
        trackableTracer.initiateRestartSpanIfNecessary(true);
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Restarted", span.getName());
        assertNull(span.getAttributes().get(START_TYPE_KEY));
    }

    @Test
    public void create_nonInitialActivity() {
        ActivityTracer trackableTracer =
                ActivityTracer.builder(mock(Activity.class))
                        .setInitialAppActivity("FirstActivity")
                        .setTracer(tracer)
                        .setAppStartupTimer(appStartupTimer)
                        .setActiveSpan(activeSpan)
                        .build();

        trackableTracer.startActivityCreation();
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Created", span.getName());
        assertNull(span.getAttributes().get(START_TYPE_KEY));
    }

    @Test
    public void create_initialActivity() {
        ActivityTracer trackableTracer =
                ActivityTracer.builder(mock(Activity.class))
                        .setInitialAppActivity("Activity")
                        .setTracer(tracer)
                        .setAppStartupTimer(appStartupTimer)
                        .setActiveSpan(activeSpan)
                        .build();
        trackableTracer.startActivityCreation();
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("AppStart", span.getName());
        assertEquals("warm", span.getAttributes().get(START_TYPE_KEY));
    }

    @Test
    public void create_initialActivity_firstTime() {
        appStartupTimer.start(tracer);
        ActivityTracer trackableTracer =
                ActivityTracer.builder(mock(Activity.class))
                        .setTracer(tracer)
                        .setAppStartupTimer(appStartupTimer)
                        .setActiveSpan(activeSpan)
                        .build();
        trackableTracer.startActivityCreation();
        trackableTracer.endActiveSpan();
        appStartupTimer.end();

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());

        SpanData appStartSpan = spans.get(0);
        assertEquals("AppStart", appStartSpan.getName());
        assertEquals("cold", appStartSpan.getAttributes().get(START_TYPE_KEY));

        SpanData innerSpan = spans.get(1);
        assertEquals("Created", innerSpan.getName());
    }

    @Test
    public void addPreviousScreen_noPrevious() {
        ActivityTracer trackableTracer =
                ActivityTracer.builder(mock(Activity.class))
                        .setTracer(tracer)
                        .setAppStartupTimer(appStartupTimer)
                        .setActiveSpan(activeSpan)
                        .build();

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(LAST_SCREEN_NAME_KEY));
    }

    @Test
    public void addPreviousScreen_currentSameAsPrevious() {
        VisibleScreenTracker visibleScreenTracker = Mockito.mock(VisibleScreenTracker.class);
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("Activity");

        ActivityTracer trackableTracer =
                ActivityTracer.builder(mock(Activity.class))
                        .setTracer(tracer)
                        .setAppStartupTimer(appStartupTimer)
                        .setActiveSpan(activeSpan)
                        .build();

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(LAST_SCREEN_NAME_KEY));
    }

    @Test
    public void addPreviousScreen() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("previousScreen");

        ActivityTracer trackableTracer =
                ActivityTracer.builder(mock(Activity.class))
                        .setTracer(tracer)
                        .setAppStartupTimer(appStartupTimer)
                        .setActiveSpan(activeSpan)
                        .build();

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertEquals("previousScreen", span.getAttributes().get(LAST_SCREEN_NAME_KEY));
    }

    @Test
    public void testScreenName() {
        ActivityTracer activityTracer =
                ActivityTracer.builder(mock(Activity.class))
                        .setTracer(tracer)
                        .setScreenName("squarely")
                        .setAppStartupTimer(appStartupTimer)
                        .setActiveSpan(activeSpan)
                        .build();
        activityTracer.startActivityCreation();
        activityTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("squarely", span.getAttributes().get(SCREEN_NAME_KEY));
    }

    private SpanData getSingleSpan() {
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        return generatedSpans.get(0);
    }
}
