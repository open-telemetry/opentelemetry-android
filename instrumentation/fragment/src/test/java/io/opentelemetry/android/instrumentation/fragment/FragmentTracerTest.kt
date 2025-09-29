/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment

import androidx.fragment.app.Fragment
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.instrumentation.common.ActiveSpan
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import io.opentelemetry.sdk.trace.data.SpanData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito

internal class FragmentTracerTest {
    private companion object {
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private lateinit var tracer: Tracer
    private lateinit var activeSpan: ActiveSpan

    @BeforeEach
    fun setup() {
        tracer = otelTesting.openTelemetry.getTracer("testTracer")
        val visibleScreenTracker =
            Mockito.mock(VisibleScreenTracker::class.java)
        activeSpan = ActiveSpan(visibleScreenTracker::previouslyVisibleScreen)
    }

    @Test
    fun create() {
        val trackableTracer =
            FragmentTracer
                .builder(Mockito.mock(Fragment::class.java))
                .setTracer(tracer)
                .setActiveSpan(activeSpan)
                .build()
        trackableTracer.startFragmentCreation()
        trackableTracer.endActiveSpan()
        val span = this.singleSpan
        assertEquals("Created", span.name)
    }

    @Test
    fun addPreviousScreen_noPrevious() {
        val trackableTracer =
            FragmentTracer
                .builder(Mockito.mock(Fragment::class.java))
                .setTracer(tracer)
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
        Mockito.`when`<String?>(visibleScreenTracker.previouslyVisibleScreen).thenReturn("Fragment")

        val trackableTracer =
            FragmentTracer
                .builder(Mockito.mock(Fragment::class.java))
                .setTracer(tracer)
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
        val visibleScreenTracker =
            Mockito.mock(VisibleScreenTracker::class.java)
        Mockito
            .`when`<String?>(visibleScreenTracker.previouslyVisibleScreen)
            .thenReturn("previousScreen")
        activeSpan = ActiveSpan(visibleScreenTracker::previouslyVisibleScreen)

        val fragmentTracer =
            FragmentTracer
                .builder(Mockito.mock(Fragment::class.java))
                .setTracer(tracer)
                .setActiveSpan(activeSpan)
                .build()

        fragmentTracer.startSpanIfNoneInProgress("starting")
        fragmentTracer.addPreviousScreenAttribute()
        fragmentTracer.endActiveSpan()

        val span = this.singleSpan
        assertEquals(
            "previousScreen",
            span.attributes.get(RumConstants.LAST_SCREEN_NAME_KEY),
        )
    }

    private val singleSpan: SpanData
        get() {
            val generatedSpans =
                otelTesting.spans
            assertEquals(1, generatedSpans.size)
            return generatedSpans[0]
        }
}
