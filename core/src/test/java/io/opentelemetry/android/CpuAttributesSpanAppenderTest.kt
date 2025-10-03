/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.os.Process
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkStatic
import io.mockk.verify
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CpuAttributesSpanAppenderTest {
    @MockK
    private lateinit var mockSpan: ReadWriteSpan

    @MockK
    private lateinit var context: Context

    val processor = CpuAttributesSpanAppender(cpuCores = 1)

    @BeforeEach
    fun setup() {
        mockkStatic(Process::class)
        every { mockSpan.setAttribute(any<AttributeKey<Double>>(), any<Double>()) } returns mockSpan
        every { mockSpan.setAttribute(any<AttributeKey<Long>>(), any<Long>()) } returns mockSpan
    }

    @Test
    fun `onStart should set the right attribute`() {
        every { Process.getElapsedCpuTime() } returns 5L

        processor.onStart(context, mockSpan)

        verify {
            mockSpan.setAttribute(RumConstants.CPU_ELAPSED_TIME_START_KEY, 5L)
        }
    }

    @Test
    fun `onEnding should set the right attributes if span has duration`() {
        every { Process.getElapsedCpuTime() } returns 50L
        every {
            mockSpan.getAttribute(RumConstants.CPU_ELAPSED_TIME_START_KEY)
        } returns 5L
        // cpuTime = 45

        every { mockSpan.latencyNanos } returns 100L * 1_000_000

        // Span took 100ms, process was active for 45ms of that time. Therefore, expect 45% cpu
        processor.onEnding(mockSpan)

        verify {
            mockSpan.setAttribute(RumConstants.CPU_AVERAGE_KEY, 45.0)
            mockSpan.setAttribute(RumConstants.CPU_ELAPSED_TIME_END_KEY, 50L)
        }

        // With multiple cores, divide CPU average, expect 22.5% cpu
        val moreCoresProcessor = CpuAttributesSpanAppender(cpuCores = 2)
        moreCoresProcessor.onEnding(mockSpan)

        verify {
            mockSpan.setAttribute(RumConstants.CPU_AVERAGE_KEY, 22.5)
            mockSpan.setAttribute(RumConstants.CPU_ELAPSED_TIME_END_KEY, 50L)
        }
    }

    @Test
    fun `onEnding should not set CPU average attribute if span has zero duration`() {
        every { Process.getElapsedCpuTime() } returns 50L
        every {
            mockSpan.getAttribute(RumConstants.CPU_ELAPSED_TIME_START_KEY)
        } returns 5L

        every { mockSpan.latencyNanos } returns 0

        processor.onEnding(mockSpan)

        verify(exactly = 0) {
            mockSpan.setAttribute(RumConstants.CPU_AVERAGE_KEY, any<Double>())
        }

        verify {
            mockSpan.setAttribute(RumConstants.CPU_ELAPSED_TIME_END_KEY, 50L)
        }
    }
}
