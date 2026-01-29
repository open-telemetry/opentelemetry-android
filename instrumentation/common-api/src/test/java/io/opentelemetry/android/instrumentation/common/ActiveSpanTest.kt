/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common

import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.api.trace.Span
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

        val span = mockk<Span>(relaxed = true)
        activeSpan.startSpan { span }
        assertTrue(activeSpan.spanInProgress())
        verify(exactly = 1) { span.makeCurrent() }

        val eventName = "event"
        activeSpan.addEvent(eventName)
        verify(exactly = 1) { span.addEvent(eventName) }

        activeSpan.endActiveSpan()
        assertFalse(activeSpan.spanInProgress())
        verify(exactly = 1) { span.makeCurrent() }
    }
}
