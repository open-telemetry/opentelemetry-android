/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer
import io.opentelemetry.android.instrumentation.common.ActiveSpan
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import io.opentelemetry.sdk.trace.data.SpanData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito

class ActivityTracerTest {
    private companion object {
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private lateinit var tracer: Tracer
    private val visibleScreenTracker: VisibleScreenTracker =
        Mockito.mock(VisibleScreenTracker::class.java)
    private val appStartupTimer = AppStartupTimer()
    private lateinit var activeSpan: ActiveSpan

    @BeforeEach
    fun setup() {
        tracer = otelTesting.openTelemetry.getTracer("testTracer")
        activeSpan = ActiveSpan(visibleScreenTracker::previouslyVisibleScreen)
    }

    @Test
    fun restart_nonInitialActivity() {
        val trackableTracer =
            ActivityTracer
                .builder(Mockito.mock(Activity::class.java))
                .setInitialAppActivity("FirstActivity")
                .setTracer(tracer)
                .setAppStartupTimer(appStartupTimer)
                .setActiveSpan(activeSpan)
                .build()
        trackableTracer.initiateRestartSpanIfNecessary(false)
        trackableTracer.endActiveSpan()
        val span = this.singleSpan
        assertEquals("Restarted", span.name)
        assertNull(span.attributes.get(RumConstants.START_TYPE_KEY))
    }

    @Test
    fun restart_initialActivity() {
        val trackableTracer =
            ActivityTracer
                .builder(Mockito.mock(Activity::class.java))
                .setInitialAppActivity("Activity")
                .setTracer(tracer)
                .setAppStartupTimer(appStartupTimer)
                .setActiveSpan(activeSpan)
                .build()
        trackableTracer.initiateRestartSpanIfNecessary(false)
        trackableTracer.endActiveSpan()
        val span = this.singleSpan
        assertEquals("AppStart", span.name)
        assertEquals("hot", span.attributes.get(RumConstants.START_TYPE_KEY))
    }

    @Test
    fun restart_initialActivity_multiActivityApp() {
        val trackableTracer =
            ActivityTracer
                .builder(Mockito.mock(Activity::class.java))
                .setInitialAppActivity("Activity")
                .setTracer(tracer)
                .setAppStartupTimer(appStartupTimer)
                .setActiveSpan(activeSpan)
                .build()
        trackableTracer.initiateRestartSpanIfNecessary(true)
        trackableTracer.endActiveSpan()
        val span = this.singleSpan
        assertEquals("Restarted", span.name)
        assertNull(span.attributes.get(RumConstants.START_TYPE_KEY))
    }

    @Test
    fun create_nonInitialActivity() {
        val trackableTracer =
            ActivityTracer
                .builder(Mockito.mock(Activity::class.java))
                .setInitialAppActivity("FirstActivity")
                .setTracer(tracer)
                .setAppStartupTimer(appStartupTimer)
                .setActiveSpan(activeSpan)
                .build()

        trackableTracer.startActivityCreation()
        trackableTracer.endActiveSpan()
        val span = this.singleSpan
        assertEquals("Created", span.name)
        assertNull(span.attributes.get(RumConstants.START_TYPE_KEY))
    }

    @Test
    fun create_initialActivity() {
        val trackableTracer =
            ActivityTracer
                .builder(Mockito.mock(Activity::class.java))
                .setInitialAppActivity("Activity")
                .setTracer(tracer)
                .setAppStartupTimer(appStartupTimer)
                .setActiveSpan(activeSpan)
                .build()
        trackableTracer.startActivityCreation()
        trackableTracer.endActiveSpan()
        val span = this.singleSpan
        assertEquals("AppStart", span.name)
        assertEquals("warm", span.attributes.get(RumConstants.START_TYPE_KEY))
    }

    @Test
    fun create_initialActivity_firstTime() {
        appStartupTimer.start(tracer)
        val trackableTracer =
            ActivityTracer
                .builder(Mockito.mock(Activity::class.java))
                .setTracer(tracer)
                .setAppStartupTimer(appStartupTimer)
                .setActiveSpan(activeSpan)
                .build()
        trackableTracer.startActivityCreation()
        trackableTracer.endActiveSpan()
        appStartupTimer.end()

        val spans = otelTesting.spans
        assertEquals(2, spans.size)

        val appStartSpan = spans[0]
        assertEquals("AppStart", appStartSpan.name)
        assertEquals("cold", appStartSpan.attributes.get(RumConstants.START_TYPE_KEY))

        val innerSpan = spans[1]
        assertEquals("Created", innerSpan.name)
    }

    @Test
    fun addPreviousScreen_noPrevious() {
        val trackableTracer =
            ActivityTracer
                .builder(Mockito.mock(Activity::class.java))
                .setTracer(tracer)
                .setAppStartupTimer(appStartupTimer)
                .setActiveSpan(activeSpan)
                .build()

        trackableTracer.startSpanIfNoneInProgress("starting")
        trackableTracer.addPreviousScreenAttribute()
        trackableTracer.endActiveSpan()

        val span = this.singleSpan
        assertNull(span.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))
    }

    @Test
    fun addPreviousScreen_currentSameAsPrevious() {
        val visibleScreenTracker =
            Mockito.mock(VisibleScreenTracker::class.java)
        Mockito.`when`(visibleScreenTracker.previouslyVisibleScreen).thenReturn("Activity")

        val trackableTracer =
            ActivityTracer
                .builder(Mockito.mock(Activity::class.java))
                .setTracer(tracer)
                .setAppStartupTimer(appStartupTimer)
                .setActiveSpan(activeSpan)
                .build()

        trackableTracer.startSpanIfNoneInProgress("starting")
        trackableTracer.addPreviousScreenAttribute()
        trackableTracer.endActiveSpan()

        val span = this.singleSpan
        assertNull(span.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY))
    }

    @Test
    fun addPreviousScreen() {
        Mockito
            .`when`(visibleScreenTracker.previouslyVisibleScreen)
            .thenReturn("previousScreen")

        val trackableTracer =
            ActivityTracer
                .builder(Mockito.mock(Activity::class.java))
                .setTracer(tracer)
                .setAppStartupTimer(appStartupTimer)
                .setActiveSpan(activeSpan)
                .build()

        trackableTracer.startSpanIfNoneInProgress("starting")
        trackableTracer.addPreviousScreenAttribute()
        trackableTracer.endActiveSpan()

        val span = this.singleSpan
        assertEquals(
            "previousScreen",
            span.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY),
        )
    }

    @Test
    fun testScreenName() {
        val activityTracer =
            ActivityTracer
                .builder(Mockito.mock(Activity::class.java))
                .setTracer(tracer)
                .setScreenName("squarely")
                .setAppStartupTimer(appStartupTimer)
                .setActiveSpan(activeSpan)
                .build()
        activityTracer.startActivityCreation()
        activityTracer.endActiveSpan()
        val span = this.singleSpan
        assertEquals("squarely", span.attributes.get(RumConstants.SCREEN_NAME_KEY))
    }

    private val singleSpan: SpanData
        get() {
            val generatedSpans =
                otelTesting.spans
            assertEquals(1, generatedSpans.size)
            return generatedSpans[0]
        }
}
