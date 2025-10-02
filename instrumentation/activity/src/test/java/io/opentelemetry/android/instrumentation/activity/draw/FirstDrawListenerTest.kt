/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.draw

import android.os.Handler
import android.view.View
import android.view.ViewTreeObserver
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FirstDrawListenerTest {
    @Test
    fun `NextDrawListener invokes callback on first draw`() {
        val view = mockk<View>(relaxed = true)
        val viewTreeObserver = mockk<ViewTreeObserver>(relaxed = true)
        val handler = mockk<Handler>(relaxed = true)

        every { view.viewTreeObserver } returns viewTreeObserver
        every { viewTreeObserver.isAlive } returns true
        every { handler.post(any()) } returns true

        var callbackInvoked = false
        var capturedView: View? = null
        val listener =
            FirstDrawListener.NextDrawListener(view, { v ->
                callbackInvoked = true
                capturedView = v
            }, handler)

        listener.onDraw()

        assert(callbackInvoked)
        assertEquals(view, capturedView)
    }

    @Test
    fun `NextDrawListener only invokes callback once`() {
        val view = mockk<View>(relaxed = true)
        val handler = mockk<Handler>(relaxed = true)

        every { handler.post(any()) } returns true

        var invocationCount = 0
        val listener =
            FirstDrawListener.NextDrawListener(view, {
                invocationCount++
            }, handler)

        listener.onDraw()
        listener.onDraw()
        listener.onDraw()

        assertEquals(1, invocationCount)
    }

    @Test
    fun `NextDrawListener removes itself via handler post`() {
        val view = mockk<View>(relaxed = true)
        val viewTreeObserver = mockk<ViewTreeObserver>(relaxed = true)
        val handler = mockk<Handler>(relaxed = true)

        every { view.viewTreeObserver } returns viewTreeObserver
        every { viewTreeObserver.isAlive } returns true

        val runnableSlot = slot<Runnable>()
        every { handler.post(capture(runnableSlot)) } returns true

        val listener = FirstDrawListener.NextDrawListener(view, {}, handler)

        listener.onDraw()

        verify { handler.post(any()) }

        runnableSlot.captured.run()

        verify { viewTreeObserver.removeOnDrawListener(listener) }
    }

    @Test
    fun `NextDrawListener does not remove if viewTreeObserver is not alive`() {
        val view = mockk<View>(relaxed = true)
        val viewTreeObserver = mockk<ViewTreeObserver>(relaxed = true)
        val handler = mockk<Handler>(relaxed = true)

        every { view.viewTreeObserver } returns viewTreeObserver
        every { viewTreeObserver.isAlive } returns false

        val runnableSlot = slot<Runnable>()
        every { handler.post(capture(runnableSlot)) } returns true

        val listener = FirstDrawListener.NextDrawListener(view, {}, handler)

        listener.onDraw()

        runnableSlot.captured.run()

        verify(exactly = 0) { viewTreeObserver.removeOnDrawListener(any()) }
    }
}
