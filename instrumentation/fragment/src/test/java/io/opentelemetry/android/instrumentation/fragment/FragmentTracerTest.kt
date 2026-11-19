/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment

import androidx.fragment.app.Fragment
import io.mockk.mockk
import io.opentelemetry.android.semconv.AppAttributes.APP_SCREEN_PREVIOUS_NAME_KEY
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import io.opentelemetry.sdk.trace.data.SpanData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class FragmentTracerTest {
    private companion object {
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private lateinit var tracer: Tracer

    @BeforeEach
    fun setup() {
        tracer = otelTesting.openTelemetry.getTracer("testTracer")
    }

    @Test
    fun create() {
        val trackableTracer =
            FragmentTracer(
                fragment = mockk<Fragment>(),
                tracer = tracer,
                screenName = "",
                lastVisibleScreen = { null },
            )
        trackableTracer.startFragmentCreation()
        trackableTracer.endActiveSpan()
        val span = this.singleSpan
        assertEquals("Created", span.name)
    }

    @Test
    fun addPreviousScreen_noPrevious() {
        val trackableTracer =
            FragmentTracer(
                fragment = mockk<Fragment>(),
                tracer = tracer,
                screenName = "",
                lastVisibleScreen = { null },
            )

        trackableTracer.startSpanIfNoneInProgress("starting")
        trackableTracer.addPreviousScreenAttribute()
        trackableTracer.endActiveSpan()

        val span = this.singleSpan
        assertNull(span.attributes.get(APP_SCREEN_PREVIOUS_NAME_KEY))
    }

    @Test
    fun addPreviousScreen_currentSameAsPrevious() {
        val trackableTracer =
            FragmentTracer(
                fragment = mockk<Fragment>(),
                tracer = tracer,
                screenName = "",
                lastVisibleScreen = { "Fragment" },
            )

        trackableTracer.startSpanIfNoneInProgress("starting")
        trackableTracer.addPreviousScreenAttribute()
        trackableTracer.endActiveSpan()

        val span = this.singleSpan
        assertNull(span.attributes.get(APP_SCREEN_PREVIOUS_NAME_KEY))
    }

    @Test
    fun addPreviousScreen() {
        val fragmentTracer =
            FragmentTracer(
                fragment = mockk<Fragment>(),
                tracer = tracer,
                screenName = "",
                lastVisibleScreen = { "previousScreen" },
            )

        fragmentTracer.startSpanIfNoneInProgress("starting")
        fragmentTracer.addPreviousScreenAttribute()
        fragmentTracer.endActiveSpan()

        val span = this.singleSpan
        assertEquals(
            "previousScreen",
            span.attributes.get(APP_SCREEN_PREVIOUS_NAME_KEY),
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
