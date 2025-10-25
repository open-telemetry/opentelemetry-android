/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.draw

import android.view.View
import android.view.Window
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class WindowUtilsTest {
    @Test
    fun `onDecorViewReady calls callback immediately when decorView exists`() {
        val window = mockk<Window>()
        val decorView = mockk<View>()
        every { window.peekDecorView() } returns decorView

        var callbackInvoked = false
        window.onDecorViewReady {
            callbackInvoked = true
        }

        assertTrue(callbackInvoked)
    }

    @Test
    fun `onDecorViewReady waits for decorView when null`() {
        val window = mockk<Window>(relaxed = true)
        val originalCallback = mockk<Window.Callback>(relaxed = true)
        every { window.peekDecorView() } returns null
        every { window.callback } returns originalCallback

        var callbackInvoked = false
        window.onDecorViewReady {
            callbackInvoked = true
        }

        assertFalse(callbackInvoked)

        val callbackSlot = slot<Window.Callback>()
        verify { window.callback = capture(callbackSlot) }

        callbackSlot.captured.onContentChanged()
        assertTrue(callbackInvoked)
    }

    @Test
    fun `onContentChanged wraps existing callback`() {
        val window = mockk<Window>(relaxed = true)
        val originalCallback = mockk<Window.Callback>(relaxed = true)
        every { window.callback } returns originalCallback

        window.onContentChanged { false }

        val callbackSlot = slot<Window.Callback>()
        verify { window.callback = capture(callbackSlot) }

        assertTrue(callbackSlot.captured is WindowDelegateCallback)
    }

    @Test
    fun `onContentChanged delegates to original callback`() {
        val window = mockk<Window>(relaxed = true)
        val originalCallback = mockk<Window.Callback>(relaxed = true)
        every { window.callback } returns originalCallback

        window.onContentChanged { false }

        val callbackSlot = slot<Window.Callback>()
        verify { window.callback = capture(callbackSlot) }

        callbackSlot.captured.onContentChanged()
        verify { originalCallback.onContentChanged() }
    }

    @Test
    fun `onContentChanged removes callback when it returns false`() {
        val window = mockk<Window>(relaxed = true)
        val originalCallback = mockk<Window.Callback>(relaxed = true)
        every { window.callback } returns originalCallback

        var invocationCount = 0
        window.onContentChanged {
            invocationCount++
            false
        }

        val callbackSlot = slot<Window.Callback>()
        verify { window.callback = capture(callbackSlot) }

        callbackSlot.captured.onContentChanged()
        assertEquals(1, invocationCount)

        callbackSlot.captured.onContentChanged()
        assertEquals(1, invocationCount)
    }

    @Test
    fun `onContentChanged keeps callback when it returns true`() {
        val window = mockk<Window>(relaxed = true)
        val originalCallback = mockk<Window.Callback>(relaxed = true)
        every { window.callback } returns originalCallback

        var invocationCount = 0
        window.onContentChanged {
            invocationCount++
            true
        }

        val callbackSlot = slot<Window.Callback>()
        verify { window.callback = capture(callbackSlot) }

        callbackSlot.captured.onContentChanged()
        assertEquals(1, invocationCount)

        callbackSlot.captured.onContentChanged()
        assertEquals(2, invocationCount)
    }
}
