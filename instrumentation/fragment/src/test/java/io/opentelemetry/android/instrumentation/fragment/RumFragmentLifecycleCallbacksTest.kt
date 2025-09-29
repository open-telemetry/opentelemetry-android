/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment

import androidx.fragment.app.Fragment
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import io.opentelemetry.sdk.trace.data.EventData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class RumFragmentLifecycleCallbacksTest {
    private companion object {
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private val visibleScreenTracker: VisibleScreenTracker =
        Mockito.mock(VisibleScreenTracker::class.java)
    private lateinit var tracer: Tracer
    private lateinit var screenNameExtractor: ScreenNameExtractor

    @BeforeEach
    fun setup() {
        tracer = otelTesting.openTelemetry.getTracer("testTracer")
        screenNameExtractor =
            mockk(relaxed = true) {
                every { extract(any()) } returns "Fragment"
            }
    }

    @Test
    fun fragmentCreation() {
        val testHarness = fragmentCallbackTestHarness

        val fragment = Mockito.mock(Fragment::class.java)
        testHarness.runFragmentCreationLifecycle(fragment)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val spanData = spans[0]

        assertEquals("Created", spanData.name)
        assertEquals(
            fragment.javaClass.simpleName,
            spanData.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertEquals(
            fragment.javaClass.simpleName,
            spanData.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(spanData.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val events = spanData.events
        assertEquals(7, events.size)
        checkEventExists(events, "fragmentPreAttached")
        checkEventExists(events, "fragmentAttached")
        checkEventExists(events, "fragmentPreCreated")
        checkEventExists(events, "fragmentCreated")
        checkEventExists(events, "fragmentViewCreated")
        checkEventExists(events, "fragmentStarted")
        checkEventExists(events, "fragmentResumed")
    }

    @Test
    fun fragmentRestored() {
        Mockito
            .`when`(visibleScreenTracker.previouslyVisibleScreen)
            .thenReturn("previousScreen")
        val testHarness = fragmentCallbackTestHarness

        val fragment = Mockito.mock(Fragment::class.java)
        testHarness.runFragmentRestoredLifecycle(fragment)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val spanData = spans[0]

        assertEquals("Restored", spanData.name)
        assertEquals(
            fragment.javaClass.simpleName,
            spanData.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertEquals(
            fragment.javaClass.simpleName,
            spanData.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertEquals(
            "previousScreen",
            spanData.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY),
        )

        val events = spanData.events
        assertEquals(3, events.size)
        checkEventExists(events, "fragmentViewCreated")
        checkEventExists(events, "fragmentStarted")
        checkEventExists(events, "fragmentResumed")
    }

    @Test
    fun fragmentResumed() {
        val testHarness = fragmentCallbackTestHarness

        val fragment = Mockito.mock(Fragment::class.java)
        testHarness.runFragmentResumedLifecycle(fragment)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val spanData = spans[0]

        assertEquals("Resumed", spanData.name)
        assertEquals(
            fragment.javaClass.simpleName,
            spanData.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertNull(spanData.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val events = spanData.events
        assertEquals(1, events.size)
        checkEventExists(events, "fragmentResumed")
    }

    @Test
    fun fragmentPaused() {
        val testHarness = fragmentCallbackTestHarness

        val fragment = Mockito.mock(Fragment::class.java)
        // calls onFragmentPaused() and onFragmentStopped()
        testHarness.runFragmentPausedLifecycle(fragment)

        val spans = otelTesting.spans
        // one paused, one stopped
        assertEquals(2, spans.size)

        val spanData = spans[0]

        assertEquals("Paused", spanData.name)
        assertEquals(
            fragment.javaClass.simpleName,
            spanData.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertEquals(
            fragment.javaClass.simpleName,
            spanData.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(spanData.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val events = spanData.events
        assertEquals(1, events.size)
        checkEventExists(events, "fragmentPaused")

        val stopSpan = spans[1]

        assertEquals("Stopped", stopSpan.name)
        assertEquals(
            fragment.javaClass.simpleName,
            stopSpan.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertEquals(
            fragment.javaClass.simpleName,
            stopSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(stopSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val stopEvents = stopSpan.events
        assertEquals(1, stopEvents.size)
        checkEventExists(stopEvents, "fragmentStopped")
    }

    @Test
    fun fragmentDetachedFromActive() {
        val testHarness = fragmentCallbackTestHarness

        val fragment = Mockito.mock(Fragment::class.java)
        testHarness.runFragmentDetachedFromActiveLifecycle(fragment)

        val spans = otelTesting.spans

        assertEquals(4, spans.size)

        val pauseSpan = spans[0]
        val stopSpan = spans[1]

        assertEquals("Paused", pauseSpan.name)
        assertEquals(
            fragment.javaClass.simpleName,
            pauseSpan.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertEquals(
            fragment.javaClass.simpleName,
            pauseSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(pauseSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        var events = pauseSpan.events
        assertEquals(1, events.size)
        checkEventExists(events, "fragmentPaused")

        assertEquals("Stopped", stopSpan.name)
        assertEquals(
            fragment.javaClass.simpleName,
            stopSpan.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertEquals(
            fragment.javaClass.simpleName,
            stopSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(stopSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val stopEvents = stopSpan.events
        assertEquals(1, stopEvents.size)
        checkEventExists(stopEvents, "fragmentStopped")

        val destroyViewSpan = spans[2]

        assertEquals("ViewDestroyed", destroyViewSpan.name)
        assertEquals(
            fragment.javaClass.simpleName,
            destroyViewSpan.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertEquals(
            fragment.javaClass.simpleName,
            destroyViewSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertNull(destroyViewSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        events = destroyViewSpan.events
        assertEquals(1, events.size)
        checkEventExists(events, "fragmentViewDestroyed")

        val detachSpan = spans[3]

        assertEquals("Destroyed", detachSpan.name)
        assertNotNull(
            detachSpan.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertNull(detachSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        events = detachSpan.events
        assertEquals(2, events.size)
        checkEventExists(events, "fragmentDestroyed")
        checkEventExists(events, "fragmentDetached")
    }

    @Test
    fun fragmentDestroyedFromStopped() {
        val testHarness = fragmentCallbackTestHarness

        val fragment = Mockito.mock(Fragment::class.java)
        testHarness.runFragmentViewDestroyedFromStoppedLifecycle(fragment)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val span = spans[0]

        assertEquals("ViewDestroyed", span.name)
        assertEquals(
            fragment.javaClass.simpleName,
            span.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertEquals(
            fragment.javaClass.simpleName,
            span.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertNull(span.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val events = span.events
        assertEquals(1, events.size)
        checkEventExists(events, "fragmentViewDestroyed")
    }

    @Test
    fun fragmentDetachedFromStopped() {
        val testHarness = fragmentCallbackTestHarness

        val fragment = Mockito.mock(Fragment::class.java)
        testHarness.runFragmentDetachedFromStoppedLifecycle(fragment)

        val spans = otelTesting.spans
        assertEquals(2, spans.size)

        val destroyViewSpan = spans[0]

        assertEquals("ViewDestroyed", destroyViewSpan.name)
        assertEquals(
            fragment.javaClass.simpleName,
            destroyViewSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertEquals(
            fragment.javaClass.simpleName,
            destroyViewSpan.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertNull(destroyViewSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        var events = destroyViewSpan.events
        assertEquals(1, events.size)
        checkEventExists(events, "fragmentViewDestroyed")

        val detachSpan = spans[1]

        assertEquals("Destroyed", detachSpan.name)
        assertEquals(
            fragment.javaClass.simpleName,
            detachSpan.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertNull(detachSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        events = detachSpan.events
        assertEquals(2, events.size)
        checkEventExists(events, "fragmentDestroyed")
        checkEventExists(events, "fragmentDetached")
    }

    @Test
    fun fragmentDetached() {
        val testHarness = fragmentCallbackTestHarness

        val fragment = Mockito.mock(Fragment::class.java)
        testHarness.runFragmentDetachedLifecycle(fragment)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)

        val detachSpan = spans[0]

        assertEquals("Detached", detachSpan.name)
        assertEquals(
            fragment.javaClass.simpleName,
            detachSpan.attributes.get(RumConstants.SCREEN_NAME_KEY),
        )
        assertEquals(
            fragment.javaClass.simpleName,
            detachSpan.attributes.get(FragmentTracer.FRAGMENT_NAME_KEY),
        )
        assertNull(detachSpan.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))

        val events = detachSpan.events
        assertEquals(1, events.size)
        checkEventExists(events, "fragmentDetached")
    }

    private fun checkEventExists(
        events: MutableList<EventData>,
        eventName: String,
    ) {
        val event = events.any { e: EventData -> e.name == eventName }
        assertTrue(event, "Event with name $eventName not found")
    }

    private val fragmentCallbackTestHarness: FragmentCallbackTestHarness
        get() =
            FragmentCallbackTestHarness(
                RumFragmentLifecycleCallbacks(
                    tracer,
                    visibleScreenTracker::previouslyVisibleScreen,
                    screenNameExtractor,
                ),
            )
}
