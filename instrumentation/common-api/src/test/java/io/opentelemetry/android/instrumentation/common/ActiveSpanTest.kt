/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ActiveSpanTest {
    @Test
    fun testActiveSpan() {
        val activeSpan = ActiveSpan { null }
        assertFalse(activeSpan.spanInProgress())

        activeSpan.endActiveSpan()
        activeSpan.addEvent("test")
        activeSpan.addPreviousScreenAttribute("screenName")
        assertFalse(activeSpan.spanInProgress())
    }

    @Test
    fun testActiveSpanCreate() {
        val activeSpan = ActiveSpan { "prev_screen" }
        assertFalse(activeSpan.spanInProgress())
        assertEquals(Context.root(), Context.current())

        val span =
            mockk<Span>(relaxed = true) {
                every { makeCurrent() } answers {
                    val span =
                        Span.wrap(
                            SpanContext.create(
                                "12345678901234561234567890123456",
                                "1234567890123456",
                                TraceFlags.getDefault(),
                                TraceState.getDefault(),
                            ),
                        )
                    span.makeCurrent()
                }
            }
        activeSpan.startSpan { span }
        assertTrue(activeSpan.spanInProgress())
        assertNotEquals(Context.root(), Context.current())

        val eventName = "event"
        activeSpan.addEvent(eventName)
        verify(exactly = 1) { span.addEvent(eventName) }

        activeSpan.endActiveSpan()
        assertFalse(activeSpan.spanInProgress())
        assertEquals(Context.root(), Context.current())
    }
}
