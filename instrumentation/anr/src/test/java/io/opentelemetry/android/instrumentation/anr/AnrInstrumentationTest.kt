/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class AnrInstrumentationTest {
    @BeforeEach
    fun setUp() {
        mockkStatic(Executors::class)
        every { Executors.newScheduledThreadPool(any()) } returns mockk<ScheduledExecutorService>(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Executors::class)
    }

    @Test
    fun `does not create scheduler when not installed`() {
        assertThat(AnrInstrumentation().name).isEqualTo("anr")

        verify(exactly = 0) { Executors.newScheduledThreadPool(any()) }
    }
}
