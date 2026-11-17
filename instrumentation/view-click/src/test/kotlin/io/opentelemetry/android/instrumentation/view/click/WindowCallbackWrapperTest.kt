/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.view.MotionEvent
import android.view.Window.Callback
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WindowCallbackWrapperTest {
    private val event = mockk<MotionEvent>()

    @Test
    fun generatorThrows_touchStillDispatched() {
        val callback = mockk<Callback>()
        every { callback.dispatchTouchEvent(event) } returns true
        val generator = mockk<ViewClickEventGenerator>()
        every { generator.generateClick(any()) } throws IllegalStateException("boom")

        val wrapper = WindowCallbackWrapper(callback, generator)

        assertThat(wrapper.dispatchTouchEvent(event)).isTrue()
        verify(exactly = 1) { callback.dispatchTouchEvent(event) }
    }

    @Test
    fun appCallbackExceptionIsNotSwallowed() {
        val callback = mockk<Callback>()
        every { callback.dispatchTouchEvent(event) } throws IllegalStateException("app bug")
        val generator = mockk<ViewClickEventGenerator>(relaxUnitFun = true)

        val wrapper = WindowCallbackWrapper(callback, generator)

        assertThatThrownBy { wrapper.dispatchTouchEvent(event) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("app bug")
    }
}
