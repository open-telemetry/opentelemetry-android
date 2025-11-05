/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import io.opentelemetry.sdk.trace.data.EventData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class ActivityCallbacksTest {
    private companion object {
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private lateinit var tracers: ActivityTracerCache
    private lateinit var visibleScreenTracker: VisibleScreenTracker

    @BeforeEach
    fun setup() {
        val tracer = otelTesting.openTelemetry.getTracer("testTracer")
        val startupTimer = AppStartupTimer()
        visibleScreenTracker = mockk<VisibleScreenTracker>(relaxed = true)
        every { visibleScreenTracker.previouslyVisibleScreen } returns null
        val extractor = mockk<ScreenNameExtractor>(relaxed = true)
        every { extractor.extract(any<Activity>()) } returns "Activity"
        tracers = ActivityTracerCache(tracer, visibleScreenTracker, startupTimer, extractor)
    }

    @Test
    fun appStartup() {
        val activityCallbacks = ActivityCallbacks(tracers)
        val testHarness = ActivityCallbackTestHarness(activityCallbacks)

        val activity = mockk<Activity>()
        testHarness.runAppStartupLifecycle(activity)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val creationSpan = spans[0]

        // TODO: ADD THIS TEST TO THE NEW COMPONENT(S)
        //        assertEquals("AppStart", startupSpan.getName());
        //        assertEquals("cold", startupSpan.getAttributes().get(SplunkRum.START_TYPE_KEY));
        assertEquals(
            activity.javaClass.simpleName,
            creationSpan.attributes.get(ActivityTracer.ACTIVITY_NAME_KEY),
        )
        assertEquals(
            activity.javaClass.simpleName,
            creationSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(creationSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val events = creationSpan.events
        assertEquals(9, events.size)

        checkEventExists(events, "activityPreCreated")
        checkEventExists(events, "activityCreated")
        checkEventExists(events, "activityPostCreated")

        checkEventExists(events, "activityPreStarted")
        checkEventExists(events, "activityStarted")
        checkEventExists(events, "activityPostStarted")

        checkEventExists(events, "activityPreResumed")
        checkEventExists(events, "activityResumed")
        checkEventExists(events, "activityPostResumed")
    }

    @Test
    fun activityCreation() {
        val activityCallbacks = ActivityCallbacks(tracers)
        val testHarness = ActivityCallbackTestHarness(activityCallbacks)
        startupAppAndClearSpans(testHarness)

        val activity = mockk<Activity>()
        testHarness.runActivityCreationLifecycle(activity)
        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val span = spans[0]

        assertEquals("AppStart", span.name)
        assertEquals("warm", span.attributes.get(RumConstants.START_TYPE_KEY))
        assertEquals(
            activity.javaClass.simpleName,
            span.attributes.get(ActivityTracer.ACTIVITY_NAME_KEY),
        )
        assertEquals(
            activity.javaClass.simpleName,
            span.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(span.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val events = span.events
        assertEquals(9, events.size)

        checkEventExists(events, "activityPreCreated")
        checkEventExists(events, "activityCreated")
        checkEventExists(events, "activityPostCreated")

        checkEventExists(events, "activityPreStarted")
        checkEventExists(events, "activityStarted")
        checkEventExists(events, "activityPostStarted")

        checkEventExists(events, "activityPreResumed")
        checkEventExists(events, "activityResumed")
        checkEventExists(events, "activityPostResumed")
    }

    private fun startupAppAndClearSpans(testHarness: ActivityCallbackTestHarness) {
        // make sure that the initial state has been set up & the application is started.
        testHarness.runAppStartupLifecycle(mockk<Activity>())
        otelTesting.clearSpans()
    }

    @Test
    fun activityRestart() {
        val activityCallbacks = ActivityCallbacks(tracers)
        val testHarness = ActivityCallbackTestHarness(activityCallbacks)

        startupAppAndClearSpans(testHarness)

        val activity = mockk<Activity>()
        testHarness.runActivityRestartedLifecycle(activity)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val span = spans[0]

        assertEquals("AppStart", span.name)
        assertEquals("hot", span.attributes.get(RumConstants.START_TYPE_KEY))
        assertEquals(
            activity.javaClass.simpleName,
            span.attributes.get(ActivityTracer.ACTIVITY_NAME_KEY),
        )
        assertEquals(
            activity.javaClass.simpleName,
            span.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(span.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val events = span.events
        assertEquals(6, events.size)

        checkEventExists(events, "activityPreStarted")
        checkEventExists(events, "activityStarted")
        checkEventExists(events, "activityPostStarted")

        checkEventExists(events, "activityPreResumed")
        checkEventExists(events, "activityResumed")
        checkEventExists(events, "activityPostResumed")
    }

    @Test
    fun activityResumed() {
        every { visibleScreenTracker.previouslyVisibleScreen } returns "previousScreen"
        val activityCallbacks = ActivityCallbacks(tracers)
        val testHarness = ActivityCallbackTestHarness(activityCallbacks)

        startupAppAndClearSpans(testHarness)

        val activity = mockk<Activity>()
        testHarness.runActivityResumedLifecycle(activity)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val span = spans[0]

        assertEquals("Resumed", span.name)
        assertEquals(
            activity.javaClass.simpleName,
            span.attributes.get(ActivityTracer.ACTIVITY_NAME_KEY),
        )
        assertEquals(
            activity.javaClass.simpleName,
            span.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertEquals(
            "previousScreen",
            span.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY),
        )

        val events = span.events
        assertEquals(3, events.size)

        checkEventExists(events, "activityPreResumed")
        checkEventExists(events, "activityResumed")
        checkEventExists(events, "activityPostResumed")
    }

    @Test
    fun activityDestroyedFromStopped() {
        val activityCallbacks = ActivityCallbacks(tracers)
        val testHarness = ActivityCallbackTestHarness(activityCallbacks)

        startupAppAndClearSpans(testHarness)

        val activity = mockk<Activity>()
        testHarness.runActivityDestroyedFromStoppedLifecycle(activity)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val span = spans[0]

        assertEquals("Destroyed", span.name)
        assertEquals(
            activity.javaClass.simpleName,
            span.attributes.get(ActivityTracer.ACTIVITY_NAME_KEY),
        )
        assertEquals(
            activity.javaClass.simpleName,
            span.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(span.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val events = span.events
        assertEquals(3, events.size)

        checkEventExists(events, "activityPreDestroyed")
        checkEventExists(events, "activityDestroyed")
        checkEventExists(events, "activityPostDestroyed")
    }

    @Test
    fun activityDestroyedFromPaused() {
        val activityCallbacks = ActivityCallbacks(tracers)
        val testHarness = ActivityCallbackTestHarness(activityCallbacks)

        startupAppAndClearSpans(testHarness)

        val activity = mockk<Activity>()
        testHarness.runActivityDestroyedFromPausedLifecycle(activity)

        val spans = otelTesting.spans
        assertEquals(2, spans.size)

        val stoppedSpan = spans[0]

        assertEquals("Stopped", stoppedSpan.name)
        assertEquals(
            activity.javaClass.simpleName,
            stoppedSpan.attributes.get(ActivityTracer.ACTIVITY_NAME_KEY),
        )
        assertEquals(
            activity.javaClass.simpleName,
            stoppedSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(stoppedSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        var events = stoppedSpan.events
        assertEquals(3, events.size)

        checkEventExists(events, "activityPreStopped")
        checkEventExists(events, "activityStopped")
        checkEventExists(events, "activityPostStopped")

        val destroyedSpan = spans[1]

        assertEquals("Destroyed", destroyedSpan.name)
        assertEquals(
            activity.javaClass.simpleName,
            destroyedSpan.attributes.get(ActivityTracer.ACTIVITY_NAME_KEY),
        )
        assertEquals(
            activity.javaClass.simpleName,
            destroyedSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(destroyedSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        events = destroyedSpan.events
        assertEquals(3, events.size)

        checkEventExists(events, "activityPreDestroyed")
        checkEventExists(events, "activityDestroyed")
        checkEventExists(events, "activityPostDestroyed")
    }

    @Test
    fun activityStoppedFromRunning() {
        val activityCallbacks = ActivityCallbacks(tracers)
        val testHarness = ActivityCallbackTestHarness(activityCallbacks)

        startupAppAndClearSpans(testHarness)

        val activity = mockk<Activity>()
        testHarness.runActivityStoppedFromRunningLifecycle(activity)

        val spans = otelTesting.spans
        assertEquals(2, spans.size)

        val stoppedSpan = spans[0]

        assertEquals("Paused", stoppedSpan.name)
        assertEquals(
            activity.javaClass.simpleName,
            stoppedSpan.attributes.get(ActivityTracer.ACTIVITY_NAME_KEY),
        )
        assertEquals(
            activity.javaClass.simpleName,
            stoppedSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(stoppedSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        var events = stoppedSpan.events
        assertEquals(3, events.size)

        checkEventExists(events, "activityPrePaused")
        checkEventExists(events, "activityPaused")
        checkEventExists(events, "activityPostPaused")

        val destroyedSpan = spans[1]

        assertEquals("Stopped", destroyedSpan.name)
        assertEquals(
            activity.javaClass.simpleName,
            destroyedSpan.attributes.get(ActivityTracer.ACTIVITY_NAME_KEY),
        )
        assertEquals(
            activity.javaClass.simpleName,
            destroyedSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(destroyedSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        events = destroyedSpan.events
        assertEquals(3, events.size)

        checkEventExists(events, "activityPreStopped")
        checkEventExists(events, "activityStopped")
        checkEventExists(events, "activityPostStopped")
    }

    private fun checkEventExists(
        events: MutableList<EventData>,
        eventName: String,
    ) {
        val event = events.any { e: EventData -> e.name == eventName }
        assertTrue(event, "Event with name $eventName not found")
    }
}
