/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
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
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension

@ExtendWith(MockKExtension::class)
internal class Pre29ActivityLifecycleCallbacksTest {
    private companion object {
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private lateinit var tracers: ActivityTracerCache

    @RelaxedMockK
    private lateinit var visibleScreenTracker: VisibleScreenTracker

    @BeforeEach
    fun setup() {
        val appStartupTimer = AppStartupTimer()
        val tracer = otelTesting.openTelemetry.getTracer("testTracer")
        val extractor = mockk<ScreenNameExtractor>(relaxed = true)
        every { extractor.extract(any<Activity>()) } returns "Activity"
        tracers = ActivityTracerCache(tracer, visibleScreenTracker, appStartupTimer, extractor)
        every { visibleScreenTracker.previouslyVisibleScreen } returns null
    }

    @Test
    fun appStartup() {
        val rumLifecycleCallbacks = Pre29ActivityCallbacks(tracers)
        val testHarness = Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks)

        val activity = mockk<Activity>()
        testHarness.runAppStartupLifecycle(activity)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val creationSpan = spans[0]

        // TODO: Add test to relevant components
        //        assertEquals("AppStart", appStartSpan.getName());
        //        assertEquals("cold", appStartSpan.getAttributes().get(SplunkRum.START_TYPE_KEY));
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
        assertEquals(3, events.size)

        checkEventExists(events, "activityCreated")
        checkEventExists(events, "activityStarted")
        checkEventExists(events, "activityResumed")
    }

    @Test
    fun activityCreation() {
        val rumLifecycleCallbacks = Pre29ActivityCallbacks(tracers)
        val testHarness = Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks)
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
        assertEquals(3, events.size)

        checkEventExists(events, "activityCreated")
        checkEventExists(events, "activityStarted")
        checkEventExists(events, "activityResumed")
    }

    private fun startupAppAndClearSpans(testHarness: Pre29ActivityCallbackTestHarness) {
        // make sure that the initial state has been set up & the application is started.
        testHarness.runAppStartupLifecycle(mockk<Activity>())
        otelTesting.clearSpans()
    }

    @Test
    fun activityRestart() {
        val rumLifecycleCallbacks = Pre29ActivityCallbacks(tracers)
        val testHarness = Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks)

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
        assertEquals(2, events.size)

        checkEventExists(events, "activityStarted")
        checkEventExists(events, "activityResumed")
    }

    @Test
    fun activityResumed() {
        every { visibleScreenTracker.previouslyVisibleScreen } returns "previousScreen"

        val rumLifecycleCallbacks = Pre29ActivityCallbacks(tracers)
        val testHarness = Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks)

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
        assertEquals(1, events.size)

        checkEventExists(events, "activityResumed")
    }

    @Test
    fun activityDestroyedFromStopped() {
        val rumLifecycleCallbacks = Pre29ActivityCallbacks(tracers)
        val testHarness = Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks)

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
        assertEquals(1, events.size)

        checkEventExists(events, "activityDestroyed")
    }

    @Test
    fun activityDestroyedFromPaused() {
        val rumLifecycleCallbacks = Pre29ActivityCallbacks(tracers)
        val testHarness = Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks)

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
        assertEquals(1, events.size)

        checkEventExists(events, "activityStopped")

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
        assertEquals(1, events.size)

        checkEventExists(events, "activityDestroyed")
    }

    @Test
    fun activityStoppedFromRunning() {
        val rumLifecycleCallbacks = Pre29ActivityCallbacks(tracers)
        val testHarness = Pre29ActivityCallbackTestHarness(rumLifecycleCallbacks)

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
        assertEquals(1, events.size)

        checkEventExists(events, "activityPaused")

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
        assertEquals(1, events.size)

        checkEventExists(events, "activityStopped")
    }

    private fun checkEventExists(
        events: MutableList<EventData>,
        eventName: String,
    ) {
        val event = events.any { e: EventData -> e.name == eventName }
        assertTrue(event, "Event with name $eventName not found")
    }
}
