/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.startup

import android.view.Choreographer
import io.mockk.every
import io.mockk.firstArg
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.TimeUnit

class TtidTimerTest {
    private companion object {
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private lateinit var tracer: Tracer
    private lateinit var anchoredClock: AnchoredClock
    private val choreographer = mockk<Choreographer>(relaxed = true)
    private val postedCallbacks = mutableListOf<Choreographer.FrameCallback>()
    private lateinit var timer: TtidTimer

    @BeforeEach
    fun setup() {
        tracer = otelTesting.openTelemetry.getTracer("test")
        anchoredClock = AnchoredClock(Clock.getDefault())
        every { choreographer.postFrameCallback(any()) } answers {
            postedCallbacks.add(firstArg())
        }
        timer = TtidTimer(tracer) { choreographer }
    }

    private fun startTimer() {
        val parentSpan = tracer.spanBuilder("AppStart").startSpan()
        timer.start(parentSpan, anchoredClock.now(), anchoredClock)
    }

    @Test
    fun `does not end the span after only the first frame callback`() {
        startTimer()
        assertEquals(1, postedCallbacks.size)

        postedCallbacks[0].doFrame(123L)

        assertEquals(2, postedCallbacks.size)
        assertTrue(otelTesting.spans.isEmpty())
    }

    @Test
    fun `ends AppStartDisplay span on the second posted frame`() {
        startTimer()
        postedCallbacks[0].doFrame(123L)
        postedCallbacks[1].doFrame(456L)

        val spans = otelTesting.spans
        assertEquals(1, spans.size)
        assertEquals(TtidTimer.SPAN_NAME, spans[0].name)
    }

    @Test
    fun `start is a no-op after the first call`() {
        val parentSpan = tracer.spanBuilder("AppStart").startSpan()
        timer.start(parentSpan, anchoredClock.now(), anchoredClock)
        timer.start(parentSpan, anchoredClock.now(), anchoredClock)

        verify(exactly = 1) { choreographer.postFrameCallback(any()) }
    }

    @Test
    fun `cancel removes the pending frame callback and the span is never ended`() {
        startTimer()
        timer.cancel()

        verify { choreographer.removeFrameCallback(postedCallbacks[0]) }
        assertTrue(otelTesting.spans.isEmpty())
    }
}
